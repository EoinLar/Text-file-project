import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.*;

public class myGUI extends JPanel implements ActionListener {

    //Declaring GUI components
    private JTextArea fileTextArea;
    private JButton open;
    private JLabel loading;
    private JButton zip;
    private JTextArea zipStats;

    private File textFile;

    private SwingWorker<Integer, String> workerLoad;
    private SwingWorker<Integer, String> workerZip;

    //Creates GUI
    public static void createAndShowGUI(){

        JFrame frame = new JFrame("File reader");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        myGUI gui = new myGUI();

        frame.setContentPane(gui.createContentPane());
        frame.setLayout(null);
        frame.setVisible(true);
    }

    //Setting position and size of GUI and GUI components
    private JPanel createContentPane() {
        JPanel totalGUI = new JPanel();
        totalGUI.setBorder(new EtchedBorder());

        totalGUI.setSize(1000, 700);
        totalGUI.setLayout(null);

        fileTextArea = new JTextArea(16,58);
        fileTextArea.setEditable(false);
        fileTextArea.setLineWrap(true);
        fileTextArea.setWrapStyleWord(true);

        JScrollPane fileText = new JScrollPane(fileTextArea);
        fileText.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        fileText.setBounds(20,100,900,500);

        zipStats = new JTextArea(10,30);
        zipStats.setEditable(true);
        zipStats.setLineWrap(true);
        zipStats.setWrapStyleWord(true);

        JScrollPane zipFileStats = new JScrollPane(zipStats);
        zipFileStats.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        zipFileStats.setBounds(150,20,500,70);

        loading = new JLabel("Loading time: ");
        loading.setBounds(20,600,250,30);

        open = new JButton("Open file");
        open.setBounds(20,20,100,30);
        open.addActionListener(this);

        zip = new JButton("Zip file");
        zip.setBounds(20,60,100,30);
        zip.setEnabled(false);
        zip.addActionListener(this);

        totalGUI.add(fileText);
        totalGUI.add(zipFileStats);
        totalGUI.add(loading);
        totalGUI.add(open);
        totalGUI.add(zip);

        return totalGUI;
    }

    /*
    Action listener reads button press.
    If open if pressed, file will be displayed on text area. Time taken to load the file is displayed.
    If zip if pressed, the selected file will be passed to the zip file method.
     */
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == open) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

            int returnVal = chooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                long startTime = System.currentTimeMillis();
                fileTextArea.setText("");
                textFile = chooser.getSelectedFile();
                load(textFile);
                long endTime = System.currentTimeMillis();
                long loadingTime = (endTime - startTime);
                loading.setText("Loading time: " + loadingTime + " mSecs");
            }
        } else if(e.getSource() == zip) {
            zip(textFile);
        }
    }
    private void load(File file) {

        workerLoad = new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                try (
                        FileReader fr = new FileReader(file);
                        BufferedReader br = new BufferedReader(fr)
                )
                {
                    String line;
                    StringBuilder fileText = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        fileText.append(line + "\r\n");
                    }
                    publish(fileText.toString());

                } catch (Exception ee) {
                    ee.printStackTrace();
                }
                zip.setEnabled(true);
                return 1;
            }

            @Override
            protected void process(List<String> dataChunks) {
                for(String lines : dataChunks) {
                    fileTextArea.append(lines + "\r\n");
                }
            }
        };
        workerLoad.execute();
    }
    /*
    File is zipped
     */
    private void zip(File textFile) {
        String fileName = textFile.getName();
        String zipFilePath = textFile.getPath().replace("txt", "zip");

        workerZip = new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                try (
                        FileOutputStream fileOut = new FileOutputStream(new File(zipFilePath));
                        ZipOutputStream zipOut = new ZipOutputStream(fileOut);
                        FileReader fr = new FileReader(fileName);
                        BufferedReader br = new BufferedReader(fr)
                )
                {
                    ZipEntry e = new ZipEntry(fileName);
                    zipOut.putNextEntry(e);

                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\r\n");
                    }
                    byte[] data = sb.toString().getBytes();
                    zipOut.write(data, 0, data.length);
                    zipOut.closeEntry();
                    zipOut.close();

                } catch (Exception ee) {
                    ee.printStackTrace();
                }
                return 1;
            }

            @Override
            protected void done() {

                Path file = Paths.get(fileName);
                Path zipFile = Paths.get(fileName.replace("txt", "zip"));

                BasicFileAttributes fileAttr = null;
                BasicFileAttributes zipFileAttr = null;
                try {
                    fileAttr = Files.readAttributes(file, BasicFileAttributes.class);
                    zipFileAttr = Files.readAttributes(zipFile, BasicFileAttributes.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String creationTime = "" + zipFileAttr.creationTime();

                creationTime = creationTime.replace("T", " ");
                creationTime = creationTime.substring(0, creationTime.indexOf("."));
                float sizeDecrease = ((float)(fileAttr.size()-zipFileAttr.size())/fileAttr.size())*100;

                StringBuilder zippedStats = new StringBuilder();
                zippedStats.append("Zip file creation time: " + creationTime + "\n\r");
                zippedStats.append("Original text file size: " + fileAttr.size() + " bytes\n\r");
                zippedStats.append("Zip file size: " + zipFileAttr.size() + " bytes\n\r");
                zippedStats.append("Percentage file size decrease: " + sizeDecrease + "%\n\r");

                zipStats.setText("" + zippedStats.toString());
            }
        };
        workerZip.execute();
    }
}