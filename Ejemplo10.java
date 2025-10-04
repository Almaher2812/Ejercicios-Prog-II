import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.List.*;
import java.util.List.*;

// ==================== Servicios base ====================
interface Service10 { String handle(String data, String user); }

class LoginService10 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

class ApiGateway10 {
    private final LoginService10 login;
    private final Map<String,Service10> routes = new HashMap<>();
    ApiGateway10(LoginService10 l){ this.login=l; }
    void register(String p, Service10 s){ routes.put(p,s); }
    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service10 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

// ==================== IU con Swing ====================
public class Ejemplo10 extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JTextArea outputArea;

    private LoginService10 loginService;
    private ApiGateway10 gateway;
    private String loggedUser;
    private String loggedPass;

    // Catálogo de cursos
    private java.util.List<String> cursos = Arrays.asList(
            "Programación I","Programación II","Estructuras","Análisis",
            "Bases de Datos","Redes","Sistemas Operativos","IA",
            "Minería de Datos","Seguridad","DevOps","Arquitectura",
            "Microservicios","Cloud","Calidad de Software"
    );

    public Ejemplo10() {
        super("🎓 Universidad - Servicios agregados");

        loginService = new LoginService10();
        gateway = new ApiGateway10(loginService);

        // Servicios internos simulados
        Service10 registro = (data,user)->"Registro OK para " + data + " ["+user+"]";
        Service10 cartera  = (data,user)->"Factura generada para " + data;

        // Agregación
        gateway.register("/uni/inscribir",(data,user)-> registro.handle(data,user) + " | " + cartera.handle(data,user));

        // Paginación
        gateway.register("/uni/cursos",(data,user)->{
            String[] p = data.split(",");
            int off = Integer.parseInt(p[0].trim());
            int lim = Integer.parseInt(p[1].trim());
            int end = Math.min(off+lim, cursos.size());
            if(off>=cursos.size()) return "[] (página vacía)";
            return cursos.subList(off,end).toString();
        });

        // Panel login
        JPanel loginPanel = new JPanel(new GridLayout(3,2));
        loginPanel.add(new JLabel("Usuario:"));
        userField = new JTextField();
        loginPanel.add(userField);
        loginPanel.add(new JLabel("Contraseña:"));
        passField = new JPasswordField();
        loginPanel.add(passField);
        loginButton = new JButton("Login");
        loginPanel.add(loginButton);

        outputArea = new JTextArea(15,40);
        outputArea.setEditable(false);

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
                "A) Inscribir curso",
                "B) Ver cursos (página 1)",
                "C) Ver cursos (página 2)",
                "D) Ver cursos (página 3)",
                "E) Ver todos los cursos",
                "F) Inscribir curso con input",
                "G) Consultar cursos con offset/limit",
                "H) Ver usuario logueado",
                "I) Ver cantidad de cursos",
                "J) Salir"
        };

        String seleccion = (String) JOptionPane.showInputDialog(
                this,
                "Seleccione una opción:",
                "Menú Universidad",
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
                resultado = gateway.request(loggedUser,loggedPass,"/uni/inscribir","Programación II");
                break;
            case 'B':
                resultado = gateway.request(loggedUser,loggedPass,"/uni/cursos","0,5");
                break;
            case 'C':
                resultado = gateway.request(loggedUser,loggedPass,"/uni/cursos","5,5");
                break;
            case 'D':
                resultado = gateway.request(loggedUser,loggedPass,"/uni/cursos","10,5");
                break;
            case 'E':
                resultado = cursos.toString();
                break;
            case 'F':
                String curso = JOptionPane.showInputDialog("Ingrese el nombre del curso a inscribir:");
                resultado = gateway.request(loggedUser,loggedPass,"/uni/inscribir",curso);
                break;
            case 'G':
                String offStr = JOptionPane.showInputDialog("Ingrese offset:");
                String limStr = JOptionPane.showInputDialog("Ingrese límite:");
                resultado = gateway.request(loggedUser,loggedPass,"/uni/cursos",offStr+","+limStr);
                break;
            case 'H':
                resultado = "👤 Usuario actual: " + loggedUser;
                break;
            case 'I':
                resultado = "📚 Total de cursos: " + cursos.size();
                break;
            case 'J':
                resultado = "👋 Saliendo...";
                JOptionPane.showMessageDialog(this,"Hasta pronto "+loggedUser+"!");
                System.exit(0);
                break;
            default:
                resultado="⚠️ Opción no implementada.";
        }

        outputArea.append(resultado+"\n");
        showMenu(); // vuelve al menú
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()-> new Ejemplo10().setVisible(true));
    }
}
