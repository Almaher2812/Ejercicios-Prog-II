import java.awt.*;
import java.time.*;
import java.util.*;
import javax.swing.*;

// Interfaz común para servicios
interface Service { 
    String handle(String data, String user); 
}

/** Servicio de login en memoria */
class LoginService1 {
    private final Map<String, String> users = new HashMap<>();

    LoginService1() {
        // 10 usuarios prediseñados
        users.put("admin", "1234");
        users.put("user", "password"); // 👈 usuario requerido
        users.put("user1", "pass1");
        users.put("user2", "pass2");
        users.put("user3", "pass3");
        users.put("user4", "pass4");
        users.put("user5", "pass5");
        users.put("user6", "pass6");
        users.put("user7", "pass7");
        users.put("user8", "pass8");
    }

    boolean login(String u, String p){ 
        return users.containsKey(u) && users.get(u).equals(p); 
    }
}

/** API Gateway con rate-limit e inventario */
class ApiGateway1 {
    private final LoginService1 login;
    private final Map<String, Service> routes = new HashMap<>();
    private final Map<String, Deque<Instant>> ventana = new HashMap<>();
    private final Map<String, Integer> inventario = new HashMap<>();
    private final int maxPorMin = 3; // Máx 3 requests por minuto

    ApiGateway1(LoginService1 l){
        this.login = l;
        // Estado inicial de inventario
        inventario.put("Avatar 2 20:00", 2);
        inventario.put("Dune 2 18:00", 1);
        inventario.put("Matrix 19:00", 3);
    }

    void register(String path, Service s){ 
        routes.put(path, s); 
    }

    Map<String,Integer> inv(){ 
        return inventario; 
    }

    /** Entrada principal */
    String request(String u, String p, String path, String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";

        // Rate-limit
        ventana.putIfAbsent(u, new ArrayDeque<>());
        Instant now = Instant.now();
        Deque<Instant> q = ventana.get(u);

        while(!q.isEmpty() && Duration.between(q.peekFirst(), now).toMinutes() >= 1) {
            q.pollFirst();
        }
        if(q.size() >= maxPorMin) return "⏳ Rate limit alcanzado para " + u;
        q.addLast(now);

        Service s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada: " + path;
        return s.handle(data, u);
    }
}

/** GUI simple con Swing */
public class Ejemplo1_Pelicula extends JFrame {
    private final LoginService1 login = new LoginService1();
    private final ApiGateway1 gw = new ApiGateway1(login);

    private JTextField userField;
    private JPasswordField passField;
    private JComboBox<String> comboPeliculas;
    private JTextArea output;

    private String currentUser;
    private String currentPass;

    public Ejemplo1_Pelicula() {
        super("🎬 Cine - Reservas");

        // Registrar endpoint
        gw.register("/cine/reservar", (data,user)->{
            int cupos = gw.inv().getOrDefault(data, 0);
            if(cupos<=0) return "😕 Sin cupos para: " + data;
            gw.inv().put(data, cupos-1);
            return "🎟️ Reserva OK ["+user+"]: " + data + " | Quedan: " + (cupos-1);
        });

        // Panel Login
        JPanel loginPanel = new JPanel(new GridLayout(3,2,5,5));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Login"));
        loginPanel.add(new JLabel("Usuario:"));
        userField = new JTextField();
        loginPanel.add(userField);

        loginPanel.add(new JLabel("Contraseña:"));
        passField = new JPasswordField();
        loginPanel.add(passField);

        JButton btnLogin = new JButton("Ingresar");
        loginPanel.add(btnLogin);

        // Panel Reservas
        JPanel cinePanel = new JPanel(new GridLayout(2,2,5,5));
        cinePanel.setBorder(BorderFactory.createTitledBorder("Reservar Función"));
        comboPeliculas = new JComboBox<>(gw.inv().keySet().toArray(new String[0]));
        JButton btnReservar = new JButton("Reservar");
        cinePanel.add(new JLabel("Seleccione función:"));
        cinePanel.add(comboPeliculas);
        cinePanel.add(btnReservar);

        // Salida
        output = new JTextArea(8,30);
        output.setEditable(false);

        // Layout principal
        setLayout(new BorderLayout());
        add(loginPanel, BorderLayout.NORTH);
        add(cinePanel, BorderLayout.CENTER);
        add(new JScrollPane(output), BorderLayout.SOUTH);

        // Acciones
        btnLogin.addActionListener(e -> {
            String u = userField.getText();
            String p = new String(passField.getPassword());
            if(login.login(u,p)){
                currentUser = u;
                currentPass = p;
                output.append("✅ Login exitoso como " + u + "\n");
            } else {
                output.append("❌ Usuario o contraseña incorrectos\n");
            }
        });

        btnReservar.addActionListener(e -> {
            if(currentUser==null){
                output.append("⚠️ Debe iniciar sesión primero\n");
                return;
            }
            String pelicula = (String) comboPeliculas.getSelectedItem();
            String resp = gw.request(currentUser, currentPass, "/cine/reservar", pelicula);
            output.append(resp + "\n");
        });

        // Config ventana
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(Ejemplo1_Pelicula::new);
    }
}
