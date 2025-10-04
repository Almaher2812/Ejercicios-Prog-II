import javax.swing.*;
import java.awt.*;
import java.util.*;

interface Service5 {
    String handle(String data, String user);
}

class LoginService5 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");

    boolean login(String u,String p){
        return users.containsKey(u) && users.get(u).equals(p);
    }
}

class ApiGateway5 {
    private final LoginService5 login;
    private final Map<String,Service5> routes = new HashMap<>();
    ApiGateway5(LoginService5 l){ this.login=l; }
    void register(String p, Service5 s){ routes.put(p,s); }
    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service5 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

public class Ejemplo5 extends JFrame {
    private LoginService5 ls = new LoginService5();
    private ApiGateway5 gw = new ApiGateway5(ls);

    // Letras para los menús
    private final String[] letras = {"A","B","C","D","E","F","G","H","I","J"};
    private final String[] nombresMenus = {
            "Pasta / Pizza / Ensalada",
            "Sushi / Ramen / Poke",
            "Carne / Pollo / Pescado",
            "Vegano / Vegetariano",
            "Hamburguesas / Papas",
            "Tacos / Burritos",
            "Ramen / Udon / Soba",
            "Postres / Helado",
            "Café / Té / Jugos",
            "Vinos / Cócteles"
    };

    public Ejemplo5(){
        // Registrar menús
        gw.register("/rest/orden1",(d,u)->"🍝 Menú A: Pasta/Pizza/Ensalada -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden2",(d,u)->"🍣 Menú B: Sushi/Ramen/Poke -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden3",(d,u)->"🥩 Menú C: Carne/Pollo/Pescado -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden4",(d,u)->"🥗 Menú D: Vegano/Vegetariano -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden5",(d,u)->"🍔 Menú E: Hamburguesas/Papas -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden6",(d,u)->"🌮 Menú F: Tacos/Burritos -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden7",(d,u)->"🍜 Menú G: Ramen/Udon/Soba -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden8",(d,u)->"🍰 Menú H: Postres/Helado -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden9",(d,u)->"☕ Menú I: Café/Té/Jugos -> Orden: " + d + " ["+u+"]");
        gw.register("/rest/orden10",(d,u)->"🍷 Menú J: Vinos/Cócteles -> Orden: " + d + " ["+u+"]");

        setTitle("Login - Restaurante");
        setSize(300,200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel de login
        JPanel panel = new JPanel(new GridLayout(3,2));
        JLabel userLabel = new JLabel("Usuario:");
        JTextField userField = new JTextField();
        JLabel passLabel = new JLabel("Contraseña:");
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Ingresar");

        panel.add(userLabel); panel.add(userField);
        panel.add(passLabel); panel.add(passField);
        panel.add(new JLabel()); panel.add(loginBtn);

        add(panel);

        // Acción del login
        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());

            if(ls.login(user,pass)){
                JOptionPane.showMessageDialog(this,"✅ Bienvenido "+user);
                mostrarMenu(user,pass);
            } else {
                JOptionPane.showMessageDialog(this,"❌ Usuario o contraseña incorrectos.");
            }
        });
    }

    private void mostrarMenu(String user,String pass){
        JFrame menuFrame = new JFrame("Menús disponibles");
        menuFrame.setSize(400,300);
        menuFrame.setLocationRelativeTo(null);

        // Crear lista con letras y nombres
        String[] menus = new String[10];
        for(int i=0;i<10;i++){
            menus[i] = letras[i] + " - " + nombresMenus[i];
        }

        JList<String> lista = new JList<>(menus);
        JScrollPane scroll = new JScrollPane(lista);

        JTextField pedidoField = new JTextField();
        JButton confirmarBtn = new JButton("Confirmar Pedido");

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(new JLabel("Ingrese su pedido:"), BorderLayout.WEST);
        abajo.add(pedidoField, BorderLayout.CENTER);
        abajo.add(confirmarBtn, BorderLayout.EAST);

        menuFrame.add(scroll, BorderLayout.CENTER);
        menuFrame.add(abajo, BorderLayout.SOUTH);

        confirmarBtn.addActionListener(e -> {
            int index = lista.getSelectedIndex();
            if(index==-1){
                JOptionPane.showMessageDialog(menuFrame,"Seleccione un menú con la letra (A-J).");
                return;
            }
            String path = "/rest/orden"+(index+1);
            String pedido = pedidoField.getText();
            String respuesta = gw.request(user,pass,path,pedido);

            menuFrame.dispose(); // Cierra ventana del menú
            mostrarResumen(user, letras[index], nombresMenus[index], pedido, respuesta);
        });

        menuFrame.setVisible(true);
    }

    private void mostrarResumen(String user, String letra, String menu, String pedido, String detalle){
        JFrame resumenFrame = new JFrame("Resumen de Pedido");
        resumenFrame.setSize(400,250);
        resumenFrame.setLocationRelativeTo(null);

        JTextArea resumen = new JTextArea();
        resumen.setEditable(false);
        resumen.setFont(new Font("Arial", Font.PLAIN, 14));

        resumen.setText(
                "📋 RESUMEN DE PEDIDO\n\n" +
                        "👤 Usuario: " + user + "\n" +
                        "🍴 Menú Seleccionado: " + letra + " - " + menu + "\n" +
                        "📝 Pedido: " + pedido + "\n\n" +
                        "✅ Confirmación:\n" + detalle
        );

        resumenFrame.add(new JScrollPane(resumen));
        resumenFrame.setVisible(true);
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new Ejemplo5().setVisible(true));
    }
}
