import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.*;

// ==================== Servicios base ====================
interface Service9 { String handle(String data, String user); }

class LoginService9 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

class ApiGateway9 {
    private final LoginService9 login;
    private final Map<String,Service9> routes = new HashMap<>();
    ApiGateway9(LoginService9 l){ this.login=l; }
    void register(String p, Service9 s){ routes.put(p,s); }
    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service9 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

// ==================== IU con Swing ====================
public class Ejemplo9 extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JTextArea outputArea;

    private LoginService9 loginService;
    private ApiGateway9 gateway;
    private String loggedUser;
    private String loggedPass;

    // Circuit Breaker interno
    enum State { CLOSED, OPEN, HALF_OPEN }
    class Breaker {
        State state = State.CLOSED;
        int fails = 0;
        Instant openedAt = null;
    }
    private Breaker cb = new Breaker();
    private Random rnd = new Random();

    public Ejemplo9() {
        super("🏨 Hotel - Circuit Breaker");

        loginService = new LoginService9();
        gateway = new ApiGateway9(loginService);

        // Endpoint de pago con breaker
        gateway.register("/hotel/pagar",(data,user)->{
            if(cb.state==State.OPEN){
                if(Duration.between(cb.openedAt,Instant.now()).getSeconds()>=5){
                    cb.state=State.HALF_OPEN; // probamos solicitud
                }else{
                    return "⛔ Circuito ABIERTO. Intente luego.";
                }
            }

            // simulamos 25% éxito
            boolean ok = rnd.nextInt(4)==0;
            if(ok){
                cb.fails=0; cb.state=State.CLOSED;
                return "💳 Pago aprobado para " + data + " ["+user+"]";
            }else{
                cb.fails++;
                if(cb.state==State.HALF_OPEN || cb.fails>=3){
                    cb.state=State.OPEN; cb.openedAt=Instant.now();
                    return "❌ Pago rechazado. Breaker ABIERTO";
                }
                return "❌ Pago rechazado. Fails=" + cb.fails;
            }
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
                "A) Pagar reserva",
                "B) Pagar otra reserva",
                "C) Ver estado del circuito",
                "D) Simular múltiples pagos",
                "E) Ver usuario logueado",
                "F) Consultar fecha/hora actual",
                "G) Mostrar política breaker",
                "H) Resetear breaker",
                "I) Mostrar historial de fallos",
                "J) Salir"
        };

        String seleccion = (String) JOptionPane.showInputDialog(
                this,
                "Seleccione una opción:",
                "Menú Hotel",
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
                resultado = gateway.request(loggedUser,loggedPass,"/hotel/pagar","Reserva Suite #777");
                break;
            case 'B':
                String reserva = JOptionPane.showInputDialog("Ingrese nombre de la reserva:");
                resultado = gateway.request(loggedUser,loggedPass,"/hotel/pagar",reserva);
                break;
            case 'C':
                resultado="🔎 Estado breaker: " + cb.state + " | Fails=" + cb.fails;
                break;
            case 'D':
                resultado="▶️ Simulación de 5 pagos:\n";
                for(int i=0;i<5;i++){
                    resultado+= gateway.request(loggedUser,loggedPass,"/hotel/pagar","Reserva Test #"+i) + "\n";
                }
                break;
            case 'E':
                resultado="👤 Usuario actual: " + loggedUser;
                break;
            case 'F':
                resultado="🕒 Hora actual: " + Instant.now();
                break;
            case 'G':
                resultado="📖 Política breaker:\n- 3 fallos cierran 5s\n- En HALF_OPEN, un fallo reabre";
                break;
            case 'H':
                cb.state=State.CLOSED; cb.fails=0; cb.openedAt=null;
                resultado="♻️ Breaker reseteado a CLOSED.";
                break;
            case 'I':
                resultado="📊 Fallos acumulados: " + cb.fails;
                break;
            case 'J':
                resultado="👋 Saliendo...";
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
        SwingUtilities.invokeLater(()-> new Ejemplo9().setVisible(true));
    }
}
