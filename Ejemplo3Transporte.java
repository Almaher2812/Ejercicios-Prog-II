import java.awt.*;
import java.time.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

/**
 * TRANSPORTE: Idempotencia (X-Idempotency-Key) + tarifa dinámica por hora pico.
 * Login: admin/1234, user/abcd
 */
interface Service3 {
    String handle(String data, String user, Map<String,String> headers);
}

/** Login simple */
class LoginService3 {
    private final Map<String,String> users = Map.of(
        "admin","1234",
        "user","abcd"
    );
    boolean login(String u,String p){
        return users.containsKey(u)&&users.get(u).equals(p);
    }
}

/** Gateway con tracking de claves idempotentes */
class ApiGateway3 {
    private final LoginService3 login;
    private final Map<String,Service3> routes = new HashMap<>();
    private final Set<String> idemUsed = new HashSet<>(); // claves ya usadas

    ApiGateway3(LoginService3 l){ this.login=l; }
    void register(String p, Service3 s){ routes.put(p,s); }

    String request(String u,String p,String path,String data, Map<String,String> headers){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service3 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u,headers);
    }

    /** Marca clave de idempotencia y retorna true si ya estaba usada */
    boolean markIdem(String key){
        if(key==null || key.isBlank()) return false;
        if(idemUsed.contains(key)) return true;
        idemUsed.add(key);
        return false;
    }
}

public class Ejemplo3Transporte {
    public static void main(String[] args){
        LoginService3 ls = new LoginService3();
        ApiGateway3 gw = new ApiGateway3(ls);

        // Rutas disponibles
        List<String> rutas = Arrays.asList(
            "Ruta 1 - Centro -> Norte",
            "Ruta 2 - Centro -> Sur",
            "Ruta 3 - Centro -> Occidente",
            "Ruta 4 - Norte -> Sur",
            "Ruta 5 - Occidente -> Oriente",
            "Ruta 6 - Aeropuerto -> Centro",
            "Ruta 7 - Terminal -> Universidad",
            "Ruta 8 - Estadio -> Parque",
            "Ruta 9 - Hospital -> Mall",
            "Ruta 10 - Ciudadela -> Centro"
        );

        // Comprar pasaje
        gw.register("/bus/pasaje",(data,user,headers)->{
            String key = headers.get("X-Idempotency-Key");
            if(key==null) return "❌ Falta X-Idempotency-Key";
            if(gw.markIdem(key)) return "✅ Compra ya registrada (idempotente).";

            LocalTime now = LocalTime.now();
            boolean pico = (now.getHour()>=6 && now.getHour()<=9) ||
                           (now.getHour()>=16 && now.getHour()<=19);
            double base = 3000, total = pico? base*1.3 : base;
            return "🎫 Ruta=" + data + " | Tarifa=" + total + (pico?" (pico)":"");
        });

        // === Interfaz gráfica ===
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sistema de Transporte");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLayout(new BorderLayout());

            // Panel de login
            JPanel loginPanel = new JPanel(new GridLayout(3,2));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            JButton loginBtn = new JButton("Ingresar");
            JLabel loginMsg = new JLabel(" ", SwingConstants.CENTER);

            loginPanel.add(new JLabel("Usuario:"));
            loginPanel.add(userField);
            loginPanel.add(new JLabel("Contraseña:"));
            loginPanel.add(passField);
            loginPanel.add(loginBtn);
            loginPanel.add(loginMsg);

            frame.add(loginPanel, BorderLayout.CENTER);

            // Panel principal
            JPanel mainPanel = new JPanel(new BorderLayout());

            JList<String> rutaList = new JList<>(rutas.toArray(new String[0]));
            mainPanel.add(new JScrollPane(rutaList), BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new GridLayout(3,1));
            JTextField idemField = new JTextField();
            JButton comprarBtn = new JButton("Comprar Pasaje");
            JLabel resultLabel = new JLabel(" ", SwingConstants.CENTER);

            bottomPanel.add(new JLabel("Clave de Idempotencia:"));
            bottomPanel.add(idemField);
            bottomPanel.add(comprarBtn);

            mainPanel.add(bottomPanel, BorderLayout.SOUTH);
            mainPanel.add(resultLabel, BorderLayout.NORTH);

            // Acción de login
            loginBtn.addActionListener(e -> {
                String user = userField.getText().trim();
                String pass = new String(passField.getPassword());
                if(ls.login(user, pass)){
                    frame.remove(loginPanel);
                    frame.add(mainPanel, BorderLayout.CENTER);
                    frame.revalidate();
                    frame.repaint();

                    // Acción de compra
                    comprarBtn.addActionListener(ev -> {
                        String rutaSel = rutaList.getSelectedValue();
                        String idem = idemField.getText().trim();
                        if(rutaSel!=null && !idem.isEmpty()){
                            Map<String,String> headers = new HashMap<>();
                            headers.put("X-Idempotency-Key", idem);
                            String res = gw.request(user, pass, "/bus/pasaje", rutaSel, headers);
                            resultLabel.setText(res);
                        } else {
                            resultLabel.setText("⚠️ Selecciona una ruta y escribe una clave.");
                        }
                    });
                } else {
                    loginMsg.setText("❌ Usuario o contraseña incorrectos.");
                }
            });

            frame.setVisible(true);
        });
    }
}
