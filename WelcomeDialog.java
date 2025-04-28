package calcoloore.gui; // Assicurati che sia la prima riga

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// Modificata: Rimosso focus painting dal pulsante
public class WelcomeDialog extends JDialog implements ActionListener {

    private JButton okButton;

    public WelcomeDialog(Frame owner) {
        super(owner, "Benvenuto", true); // Modale

        // Contenuto del dialogo
        JLabel welcomeLabel = new JLabel("Benvenuto a Gestione Ore Lavoro!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        okButton = new JButton("Avanti");
        okButton.setFocusPainted(false); // *** AGGIUNTO ***
        okButton.addActionListener(this);

        // Layout
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.add(welcomeLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(panel);

        // Impostazioni finestra
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Quando il pulsante viene premuto, chiude semplicemente il dialogo
        dispose();
    }

    // Metodo statico non più usato nel main attuale, ma può rimanere
    public static void showWelcomeDialog(Frame owner) {
        WelcomeDialog dialog = new WelcomeDialog(owner);
        dialog.setVisible(true);
    }
}