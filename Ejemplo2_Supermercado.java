import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

/**
 * SUPERMERCADO: Carrito por usuario + subtotal + cupones + auditoría.
 * Login: admin/1234, user/password
 *
 * Demuestra:
 * - Estado por usuario (carrito)
 * - Precios, cupones, cálculo de subtotal/total
 * - Bitácora de acciones (auditoría)
 */
interface Service2 { 
    String handle(String data, String user, Map<String,String> headers); 
}

/** Login simple en memoria */
class LoginService2 {
    private final Map<String,String> users = new HashMap<>();
    
    LoginService2(){
        users.put("admin","1234");
        users.put("user","password");  // login prediseñado
    }
    
    boolean login(String u,String p){ 
        return users.containsKey(u)&&users.get(u).equals(p); 
    }
}

/** Gateway con rutas, carritos y auditoría */
class ApiGateway2 {
    private final LoginService2 login;
    private final Map<String, Service2> routes = new HashMap<>();
    // Carritos por usuario
    private final Map<String, List<String>> carritos = new HashMap<>();
    // Lista de precios base
    private final Map<String, Double> precios = new HashMap<>();
    // Cupones válidos
    private final Set<String> cupones = new HashSet<>();
    // Auditoría (acciones realizadas)
    private final List<String> audit = new ArrayList<>();

    ApiGateway2(LoginService2 l){
        this.login = l;
        // Precios de 10 productos
        precios.put("Leche",3.5); 
        precios.put("Pan",2.0);
        precios.put("Huevos",4.0); 
        precios.put("Arroz",3.0);
        precios.put("Azúcar",2.5);
        precios.put("Café",5.0);
        precios.put("Carne",12.0);
        precios.put("Pollo",9.5);
        precios.put("Queso",7.0);
        precios.put("Manzanas",4.5);

        // Cupones
        cupones.add("DESC10");
        cupones.add("DESC20");
    }

    void register(String p, Service2 s){ routes.put(p,s); }

    /** Valida login y rutea a la acción */
    String request(String u,String p,String path,String data, Map<String,String> headers){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service2 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u,headers);
    }

    // Getters
    Map<String,List<String>> carts(){ return carritos; }
    Map<String,Double> price(){ return precios; }
    Set<String> coupons(){ return cupones; }
    List<String> log(){ return audit; }
}

/** Clase principal con interfaz gráfica */
public class Ejemplo2_Supermercado {
    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            LoginService2 ls = new LoginService2();
            ApiGateway2 gw = new ApiGateway2(ls);

            // Rutas
            gw.register("/super/add",(data,user,headers)->{
                gw.carts().computeIfAbsent(user,k->new ArrayList<>()).add(data);
                gw.log().add("ADD "+user+" -> "+data);
                return "✅ Agregado al carrito: " + data;
            });

            gw.register("/super/checkout",(data,user,headers)->{
                List<String> items = gw.carts().getOrDefault(user, new ArrayList<>());
                double subtotal = 0;
                for(String it: items) subtotal += gw.price().getOrDefault(it,1.0);
                double desc = gw.coupons().contains(data) ? (data.equals("DESC20")?0.20:0.10) : 0.0;
                double total = Math.round(subtotal*(1-desc)*100.0)/100.0;
                gw.log().add("CHECKOUT "+user+" subtotal="+subtotal+" cupon="+data+" total="+total);
                return "🧾 Items="+items+" | Subtotal="+subtotal+" | Cupón="+(desc>0?data:"N/A")+" | Total="+total;
            });

            // Interfaz gráfica
            JFrame loginFrame = new JFrame("Login Supermercado");
            loginFrame.setSize(300,200);
            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loginFrame.setLayout(new GridLayout(3,2));

            JLabel userLabel = new JLabel("Usuario:");
            JTextField userField = new JTextField();
            JLabel passLabel = new JLabel("Contraseña:");
            JPasswordField passField = new JPasswordField();
            JButton loginBtn = new JButton("Ingresar");
            JLabel msgLabel = new JLabel("");

            loginFrame.add(userLabel);
            loginFrame.add(userField);
            loginFrame.add(passLabel);
            loginFrame.add(passField);
            loginFrame.add(loginBtn);
            loginFrame.add(msgLabel);

            loginFrame.setVisible(true);

            loginBtn.addActionListener(e -> {
                String user = userField.getText();
                String pass = new String(passField.getPassword());

                if(ls.login(user,pass)){
                    loginFrame.dispose();
                    mostrarSupermercado(gw,user,pass);
                } else {
                    msgLabel.setText("❌ Credenciales inválidas");
                }
            });
        });
    }

    private static void mostrarSupermercado(ApiGateway2 gw, String user, String pass){
        JFrame frame = new JFrame("Supermercado - Usuario: " + user);
        frame.setSize(500,400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        DefaultListModel<String> productModel = new DefaultListModel<>();
        for(String prod: gw.price().keySet()) productModel.addElement(prod);
        JList<String> productList = new JList<>(productModel);
        JScrollPane scroll = new JScrollPane(productList);

        JButton addBtn = new JButton("Agregar al carrito");
        JButton checkoutBtn = new JButton("Checkout");
        JTextField couponField = new JTextField();
        JLabel resultLabel = new JLabel("Bienvenido al supermercado!");

        JPanel bottomPanel = new JPanel(new GridLayout(3,1));
        bottomPanel.add(addBtn);
        bottomPanel.add(new JLabel("Cupón Escriva el Cupon: (DESC10/DESC20)"));
        bottomPanel.add(couponField);
        bottomPanel.add(checkoutBtn);

        frame.add(scroll, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(resultLabel, BorderLayout.NORTH);

        addBtn.addActionListener(e -> {
            String selected = productList.getSelectedValue();
            if(selected!=null){
                String res = gw.request(user,pass,"/super/add",selected,Map.of());
                resultLabel.setText(res);
            }
        });

        checkoutBtn.addActionListener(e -> {
            String coupon = couponField.getText().trim();
            String res = gw.request(user,pass,"/super/checkout",coupon,Map.of());
            resultLabel.setText(res);
        });

        frame.setVisible(true);
    }
}
