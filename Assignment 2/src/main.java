public class main {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                myGUI gui = new myGUI();
                gui.createAndShowGUI();
            }
        });
    }
}
