import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

// ==================== Servicios base ====================
interface Service8 { String handle(String data, String user); }

class LoginService8 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

class ApiGateway8 {
    private final LoginService8 login;
    private final Map<String,Service8> routes = new HashMap<>();
    final Map<String,Integer> cupos = new HashMap<>();

    ApiGateway8(LoginService8 l){ this.login=l; }
    void register(String p, Service8 s){ routes.put(p,s); }

    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service8 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

// ==================== IU con Swing ====================
public class Ejemplo8 extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JTextArea outputArea;

    private LoginService8 loginService;
    private ApiGateway8 gateway;
    private String loggedUser;
    private String loggedPass;

    public Ejemplo8() {
        super("🏋️ Gimnasio RBAC - Login & Menú");

        loginService = new LoginService8();
        gateway = new ApiGateway8(loginService);

        // Registro de rutas
        gateway.register("/gym/create",(data,user)->{
            if(!"admin".equals(user)) return "🚫 Solo admin puede crear clases.";
            String[] p = data.split(":");
            gateway.cupos.put(p[0], Integer.parseInt(p[1]));
            return "✅ Clase creada: " + p[0] + " (" + p[1] + " cupos)";
        });
        gateway.register("/gym/inscribir",(data,user)->{
            int c = gateway.cupos.getOrDefault(data,0);
            if(c<=0) return "❌ Sin cupos en " + data;
            gateway.cupos.put(data,c-1);
            return "🎫 " + user + " inscrito en " + data + " | Quedan: " + (c-1);
        });

        // Panel Login
        JPanel loginPanel = new JPanel(new GridLayout(3,2));
        loginPanel.add(new JLabel("Usuario:"));
        userField = new JTextField();
        loginPanel.add(userField);
        loginPanel.add(new JLabel("Contraseña:"));
        passField = new JPasswordField();
        loginPanel.add(passField);
        loginButton = new JButton("Login");
        loginPanel.add(loginButton);

        // Output
        outputArea = new JTextArea(15,40);
        outputArea.setEditable(false);

        // Eventos
        loginButton.addActionListener(e->login());

        setLayout(new BorderLayout());
        add(loginPanel, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void login() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if(loginService.login(user,pass)) {
            loggedUser=user;
            loggedPass=pass;
            outputArea.setText("✅ Bienvenido " + user + "\n");
            showMenu();
        } else {
            JOptionPane.showMessageDialog(this,"❌ Usuario o contraseña inválidos.");
        }
    }

    private void showMenu() {
        String[] opciones = {
                "A) Crear clase",
                "B) Inscribirse en clase",
                "C) Ver cupos disponibles",
                "D) Ver clases creadas",
                "E) Crear clase (extra 1)",
                "F) Crear clase (extra 2)",
                "G) Inscribirse en otra clase",
                "H) Consultar estado usuario",
                "I) Ver historial de inscripciones",
                "J) Salir"
        };

        String seleccion = (String) JOptionPane.showInputDialog(
                this,
                "Seleccione una opción:",
                "Menú Gimnasio",
                JOptionPane.PLAIN_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        if(seleccion!=null) handleSelection(seleccion.charAt(0));
    }

    private void handleSelection(char opcion) {
        String resultado = "";
        switch(opcion) {
            case 'A':
                if("admin".equals(loggedUser)) {
                    String clase = JOptionPane.showInputDialog("Ingrese clase y cupos (ej: Crossfit:3)");
                    resultado = gateway.request(loggedUser,loggedPass,"/gym/create",clase);
                } else resultado="🚫 Solo admin puede crear clases.";
                break;
            case 'B':
                String clase = JOptionPane.showInputDialog("Ingrese nombre de la clase:");
                resultado = gateway.request(loggedUser,loggedPass,"/gym/inscribir",clase);
                break;
            case 'C':
                resultado="📋 Cupos:\n";
                for(var e: gateway.cupos.entrySet())
                    resultado += e.getKey()+" → "+e.getValue()+"\n";
                break;
            case 'D':
                resultado="📌 Clases creadas:\n";
                for(String c: gateway.cupos.keySet()) resultado+=c+"\n";
                break;
            case 'J':
                resultado="👋 Saliendo del sistema.";
                JOptionPane.showMessageDialog(this,"Hasta pronto "+loggedUser+"!");
                System.exit(0);
                break;
            default:
                resultado="⚠️ Opción aún no implementada.";
        }

        outputArea.append(resultado+"\n");
        showMenu(); // vuelve al menú
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()-> new Ejemplo8().setVisible(true));
    }
}
