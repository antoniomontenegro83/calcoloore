package calcoloore.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

// Modificata: Rimosso focus painting dai pulsanti
public class LoginDialog extends JDialog implements ActionListener {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    private boolean loginSuccess = false;

    public LoginDialog(Frame owner) {
        super(owner, "Login - Gestione Ore Lavoro", true);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font dialogFont = new Font("SansSerif", Font.PLAIN, 13);

        // Riga Utente
        JLabel userLabel = new JLabel("Utente:");
        userLabel.setFont(dialogFont);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        add(userLabel, gbc);

        usernameField = new JTextField(15);
        usernameField.setFont(dialogFont);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        add(usernameField, gbc);

        // Riga Password
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(dialogFont);
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        add(passLabel, gbc);

        passwordField = new JPasswordField(15);
        passwordField.setFont(dialogFont);
        passwordField.addActionListener(this);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        add(passwordField, gbc);

        // Riga Status/Errori
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(dialogFont.deriveFont(Font.ITALIC));
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.0;
        gbc.insets = new Insets(5, 10, 5, 10);
        add(statusLabel, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 5, 10);

        // Riga Pulsanti
        loginButton = new JButton("Login");
        loginButton.setFont(dialogFont);
        loginButton.setFocusPainted(false); // *** AGGIUNTO ***
        loginButton.addActionListener(this);

        cancelButton = new JButton("Annulla");
        cancelButton.setFont(dialogFont);
        cancelButton.setFocusPainted(false); // *** AGGIUNTO ***
        cancelButton.addActionListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getRootPane().setDefaultButton(loginButton);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                usernameField.requestFocusInWindow();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // ... (actionPerformed invariato) ...
        Object source = e.getSource();

        if (source == loginButton || source == passwordField) {
            String username = usernameField.getText().trim();
            char[] password = passwordField.getPassword();
            // --- Logica Autenticazione Fittizia ---
            boolean authenticated = username.equals("admin") && Arrays.equals(password, "password".toCharArray());
            Arrays.fill(password, '0');

            if (authenticated) {
                loginSuccess = true;
                loadMainApplicationWindow();
                dispose();
            } else {
                statusLabel.setText("Utente o password non validi.");
                loginSuccess = false;
                passwordField.setText("");
                usernameField.requestFocusInWindow();
            }
        } else if (source == cancelButton) {
            loginSuccess = false;
            dispose();
        }
    }

    public boolean isLoginSuccessful() {
        return loginSuccess;
    }

    private void loadMainApplicationWindow() {
       // ... (loadMainApplicationWindow invariato) ...
       System.out.println("[DEBUG LoginDialog] Autenticazione OK. Caricamento DashboardFrame...");
       try {
            DashboardFrame dashboard = new DashboardFrame();
            dashboard.setVisible(true);
       } catch (Exception e) {
           e.printStackTrace();
           JOptionPane.showMessageDialog(this, "Errore imprevisto durante l'avvio della Dashboard:\n" + e.getMessage(), "Errore Avvio", JOptionPane.ERROR_MESSAGE);
           loginSuccess = false;
       }
    }
}