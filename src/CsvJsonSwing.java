package src;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.Component;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CsvJsonSwing extends JFrame {

    private static final long serialVersionUID = 1L;
    private JTable table;


    private final int WIDTH = 1200;
    private final int HEIGHT = 800;

    private File lastLoadedFile; // Update para lastLoadedFile para ser mais claro

    private JPanel panel;

    //Se houver mais de 1 panel, é necessário criar um subPanel array?
    private JPanel subPanel;

    private JComboBox<String> sortComboBox;

    Horario horario;

    String source ;





    public CsvJsonSwing()  {

        super("ISCTE Schedule Generator");

        panelRequirement();

        //SubPanel para os butões no BorderLayout.SOUTH, no panel do panelRequirement()
        subPanel = new JPanel();




        //Não confundir com a table com model, isto apenas abre espaço para o model
        showPreviewTable();


       //Butons
        loadFileButton();
        convertHTMLButton();
        openConvertedFileButton();
        loadFromURIButton();
        setOverlapButton();
        setOverCrowdedButton();
        sortButton();
        resetButton();
        filterButton();

    }
    public void loadFile(String filename) throws IOException {
        source = filename;
        Utils.clearColors(table);
        boolean isCsv = filename.endsWith(".csv");
        if (isCsv) {
            horario = Utils.csvToHorario(filename);
        }
        else {
            horario = Utils.parseJson(filename);
        }
        DefaultTableModel model = getModel(horario);
        displayModel(model);



    }

    //fazer get model de modo a que receba um horario e a que nao receba um horario para o inicial
    public DefaultTableModel getModel(Horario horario){
        List<Object[]> data = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();

        //Método auxiliar para adicionar colunas
        columnAdder((ArrayList<String>) columnNames);


        data.add(columnNames.toArray(new String[columnNames.size()]));
        for (Bloco bloco : horario.horario) {
            List<Object> row = new ArrayList<>();
            row.add(bloco.getUc());
            row.add(bloco.getTurma());
            row.add(bloco.getCurso());
            row.add(bloco.getDia_sem());
            row.add(bloco.getSala());
            row.add(bloco.getMaxSala());
            row.add(bloco.getnInscritos());
            row.add(bloco.getHoraInicioUC());
            row.add(bloco.getHoraFimUC());
            row.add(bloco.getDataAula());
            row.add(bloco.getTurno());
            data.add(row.toArray(new Object[row.size()]));
        }
        DefaultTableModel model = new DefaultTableModel(data.toArray(new Object[0][0]), data.get(0));
        model.removeRow(0);
       return model;
    }

    public void displayModel(DefaultTableModel model){
        table.setModel(model);
    }

    public void loadFileFromUri(String uri) {
        source = uri;
        try {
            if (uri.startsWith("https://")) {
                CalendarBuilder builder = new CalendarBuilder();
                Calendar calendar = builder.build(new URL(uri).openStream());
                List<String[]> data = new ArrayList<>();
                for (Object event : calendar.getComponents(Component.VEVENT)) {
                    String[] fields = new String[4];
                    fields[0] = ((VEvent) event).getSummary().getValue();
                    fields[1] = ((VEvent) event).getStartDate().getValue().toString();
                    fields[2] = ((VEvent) event).getEndDate().getValue().toString();
                    fields[3] = ((VEvent) event).getDescription().toString();
                    data.add(fields);
                }
                String[] headers = {"UC", "Start Date (Format: yy/dd/mm)", "End Date (Format: yy/dd/mm)", "Summary"};
                DefaultTableModel model = new DefaultTableModel(headers, 0);
                for (String[] row : data) {
                    model.addRow(row);
                }
                table.setModel(model);
            } else {
                JOptionPane.showMessageDialog(this,"Invalid Protocol. Only https:// is supported. If using webcal link please change the start to https://. Webcal is not secured and is not supported by the IANA.");
            }
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this, "Invalid URI: " + uri);
            e.printStackTrace();
        } catch (IOException | ParserException e) {
            JOptionPane.showMessageDialog(this, "Error loading file from URI: " + e.getMessage());
            e.printStackTrace();
        }
    }










    private void convertToHTML(String filename, int sortOption) throws IOException {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object[] row = new Object[model.getColumnCount()];
            for (int j = 0; j < model.getColumnCount(); j++) {
                row[j] = model.getValueAt(i, j);
            }
            rows.add(row);
        }

        int dataAulaColumnIndex = model.findColumn("dataAula");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


        Comparator<Object[]> comparator = null;
        switch (sortOption) {
            case 0: // Sort by Day
                comparator = (a, b) -> {
                    LocalDate aDate = LocalDate.parse((String) a[dataAulaColumnIndex], dateFormatter);
                    LocalDate bDate = LocalDate.parse((String) b[dataAulaColumnIndex], dateFormatter);
                    return aDate.compareTo(bDate);
                };
                break;
            case 1: // Sort by Week
                comparator = (a, b) -> {
                    LocalDate aDate = LocalDate.parse((String) a[dataAulaColumnIndex], dateFormatter);
                    LocalDate bDate = LocalDate.parse((String) b[dataAulaColumnIndex], dateFormatter);
                    LocalDate aWeek = aDate.with(DayOfWeek.MONDAY);
                    LocalDate bWeek = bDate.with(DayOfWeek.MONDAY);
                    return aWeek.compareTo(bWeek);
                };
                break;
            case 2: // Sort by Month
                comparator = (a, b) -> {
                    LocalDate aDate = LocalDate.parse((String) a[dataAulaColumnIndex], dateFormatter);
                    LocalDate bDate = LocalDate.parse((String) b[dataAulaColumnIndex], dateFormatter);
                    YearMonth aMonth = YearMonth.from(aDate);
                    YearMonth bMonth = YearMonth.from(bDate);
                    return aMonth.compareTo(bMonth);
                };
                break;
        }

        // Sort the rows using the comparator
        rows.sort(comparator);

        // Write the sorted data to the HTML file
        String html = "<html><head>\n" +
                "  <link rel=\"stylesheet\" href=\"styles.css\">\n" +
                "</head><body><table border='1'>";

        for (int i = 0; i < model.getColumnCount(); i++) {
            html += "<th>" + model.getColumnName(i) + "</th>";
        }
        for (int i = 0; i < rows.size(); i++) {
            html += "<tr>";
            Object[] row = rows.get(i);
            for (int j = 0; j < row.length; j++) {
                html += "<td>" + row[j] + "</td>";
            }
            html += "</tr>";
        }
        html += "</table></body></html>";

        String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
        Path path = Paths.get(filenameWithoutExtension + ".html");
        lastLoadedFile = new File(path.toString());
        Files.write(path, html.getBytes());
        JOptionPane.showMessageDialog(this, "File converted successfully: " + path.toAbsolutePath());
    }









    /*public  DefaultTableModel tableTOModel(){
        TableModel model =table.getModel();
        DefaultTableModel defaultModel = new DefaultTableModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
           defaultModel.addColumn(model.getColumnName(i));
        }
        for (int i = 0; i < model.getRowCount(); i++) {
            Object[] row = new Object[model.getColumnCount()];
            for (int j = 0; j < model.getColumnCount(); j++) {
                row[j] = model.getValueAt(i, j);
            }
            defaultModel.addRow(row);
        }

        return defaultModel;

    }*/











    /********************************************************************
     * SWING CODE (functions for GUI, auxiliary functincs, buttons, etc)*
     ********************************************************************/

    public void panelRequirement(){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(WIDTH, HEIGHT));
        setLocationRelativeTo(null);
        panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(panel);
    }

    public void showPreviewTable(){
        table = new JTable();
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
    }


    public void loadFileButton() {

        JButton loadButton = new JButton("Load File");
        loadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV/JSON Files", "csv", "json"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filename = fileChooser.getSelectedFile().getAbsolutePath();
                lastLoadedFile = new File(filename); // Update the lastLoadedFile variable
                try {
                    loadFile(filename);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        subPanel.add(loadButton);
    }

    private void resetTable()throws IOException {
        boolean isCsv = source.endsWith(".csv");
        if (isCsv) {
            horario = Utils.csvToHorario(source);
        }
        else {
            horario = Utils.parseJson(source);
        }
        DefaultTableModel model = getModel(horario);
        displayModel(model);

    }



    public void convertHTMLButton(){

        JButton convertButton = new JButton("Convert to HTML");
        convertButton.addActionListener(e -> {
            if (lastLoadedFile != null) { // Update the variable name to lastLoadedFile
                try {
                    convertToHTML(lastLoadedFile.getAbsolutePath(), sortComboBox.getSelectedIndex());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error converting file to HTML: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "No file has been loaded to convert.");
            }
        });
        panel.add(convertButton, BorderLayout.EAST);
    }

    public void openConvertedFileButton(){
        JButton openButton = new JButton("Open Converted File");
        openButton.addActionListener(e -> {
            if (lastLoadedFile != null) { // Update the variable name to lastLoadedFile
                try {
                    Desktop.getDesktop().open(lastLoadedFile);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error opening converted file: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "No converted file has been generated.");
            }
        });
        panel.add(openButton, BorderLayout.WEST);
    }


    public void loadFromURIButton(){
        JButton loadFromUriButton = new JButton("Load from URI");
        loadFromUriButton.addActionListener(e -> {
            String uri = JOptionPane.showInputDialog(this, "Enter URI (use https:// instead of webcal:// for security reasons):");
            if (uri != null) {
                loadFileFromUri(uri);
            }else{
                JOptionPane.showMessageDialog(this, "Null input, no preview generated.");
            }
        });

        subPanel.add(loadFromUriButton);
        panel.add(subPanel, BorderLayout.SOUTH);

    }
    public void setOverlapButton(){
        JButton loadFromUriButton = new JButton("Mostrar Sobreposição");
        loadFromUriButton.addActionListener(e -> {
            Utils.setOverlap(horario,table);
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {

                    }

                }
            });
        });

        subPanel.add(loadFromUriButton);
        panel.add(subPanel, BorderLayout.SOUTH);

    }
    public void setOverCrowdedButton(){
        JButton loadFromUriButton = new JButton("Mostrar Sobrelotação");
        loadFromUriButton.addActionListener(e -> {
            Utils.setOverCrowded(horario,table);
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {

                    }

                }
            });
        });

        subPanel.add(loadFromUriButton);
        panel.add(subPanel, BorderLayout.SOUTH);

    }
    public void sortButton(){
        JButton loadFromUriButton = new JButton("sort");
        loadFromUriButton.addActionListener(e -> {
            horario = Utils.sortByDate(horario);

            displayModel(getModel(horario));
        });

        subPanel.add(loadFromUriButton);
        panel.add(subPanel, BorderLayout.SOUTH);

    }
    public void resetButton(){
        JButton loadFromUriButton = new JButton("reset");
        loadFromUriButton.addActionListener(e -> {
            try {
                resetTable();
                Utils.clearColors(table);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        subPanel.add(loadFromUriButton);
        panel.add(subPanel, BorderLayout.SOUTH);

    }
    public void filterButton(){
        JButton loadFromUriButton = new JButton("filter");
        loadFromUriButton.addActionListener(e -> {

            filter();
        });

        subPanel.add(loadFromUriButton);
        panel.add(subPanel, BorderLayout.SOUTH);

    }
    void filter(){
        JFrame frame = new JFrame("Filtro");
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("DATA");
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setPreferredSize(new Dimension(300, 40));
        panel.add(label);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel labelDay = new JLabel("Dia:");
        inputPanel.add(labelDay);
        JTextField textFieldDay = new JTextField(2);
        inputPanel.add(textFieldDay);

        JLabel labelMonth = new JLabel("Mês:");
        inputPanel.add(labelMonth);
        JTextField textFieldMonth = new JTextField(2);
        inputPanel.add(textFieldMonth);

        JLabel labelYear = new JLabel("Ano:");
        inputPanel.add(labelYear);
        JTextField textFieldYear = new JTextField(4);
        inputPanel.add(textFieldYear);

        panel.add(inputPanel);

        String[] options = {"Dia", "Semana", "Mes"};
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setAlignmentX(JComboBox.CENTER_ALIGNMENT);
        comboBox.setMaximumSize(new Dimension(200, 30));
        panel.add(comboBox);

        JButton filterButton = new JButton("Filtrar");
        filterButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        filterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) comboBox.getSelectedItem();
                int day = Integer.parseInt(textFieldDay.getText());
                int month = Integer.parseInt(textFieldMonth.getText()) ;
                int  year =Integer.parseInt(textFieldYear.getText()) ;

                String formattedD = String.format("%02d", day);
                String formattedM = String.format("%02d", month);
                String formatedY = textFieldYear.getText() ;
                if(String.valueOf(year).length() == 2){
                    formatedY = "20"+year;
                }

                String date = formattedD +"/"+formattedM+"/"+formatedY;
                System.out.println(date);

                Horario filtred = Utils.filter(horario,selectedItem,date);
                displayModel(getModel(filtred));


                frame.dispose();

            }
        });
        panel.add(filterButton);

        frame.add(panel);
        frame.setVisible(true);
    }





    /********************************************************************
     * Auxiliary Functions                                              *
     ********************************************************************/

    public void columnAdder(ArrayList<String> columnNames){
        columnNames.add("uc");
        columnNames.add("turma");
        columnNames.add("curso");
        columnNames.add("dia_sem");
        columnNames.add("sala");
        columnNames.add("maxSala");
        columnNames.add("nInscritos");
        columnNames.add("horaInicioUC");
        columnNames.add("horaFimUC");
        columnNames.add("dataAula");
        columnNames.add("turno");
    }
















    //Para mostrar que http não funciona pois o link é movido para https e altera o conteudo para html e o parser do iCalendar não funciona
    //Webcal também não funciona pois não é um protocolo reconhecido pela IANA logo dava o erro de URLMalformation e tinha de usar várias libraries para contornar isso e ainda por cima era depois traduzido para HTTPS
    //logo não faz sentido usar webcal sequer.
    /*private void loadFileFromUri2(String uri) {
        try {
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();
            if (uri.startsWith("webcal://")) {
                String httpUrl = connection.getHeaderField("Location");
                connection = new URL(httpUrl).openConnection();
            }
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            List<String[]> data = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                data.add(fields);
            }
            reader.close();
            inputStream.close();

            String[] headers = data.remove(0);
            DefaultTableModel model = new DefaultTableModel(headers, 0);
            for (String[] row : data) {
                model.addRow(row);
            }
            table.setModel(model);

        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this, "Invalid URI: " + uri);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading file from URI: " + e.getMessage());
        }
    }
    */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CsvJsonSwing test = new CsvJsonSwing();
            test.setVisible(true);
        });
    }
}