import javax.swing.*;
import java.time.*;
import java.util.*;

// === Servicios ===
interface Service7 { String handle(String data, String user); }

class LoginService7 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

class ApiGateway7 {
    private final LoginService7 login;
    private final Map<String,Service7> routes = new HashMap<>();
    final Map<String,Integer> stock = new HashMap<>();
    final Deque<Instant> ventana = new ArrayDeque<>(); // rate limit

    ApiGateway7(LoginService7 l){
        this.login=l;
        stock.put("Paracetamol 500",2);
        stock.put("Antibiótico X",1);
        stock.put("Jarabe Infantil",3);
    }

    void register(String p, Service7 s){ routes.put(p,s); }

    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";

        // Rate limit solo en /farmacia/pedir
        if("/farmacia/pedir".equals(path)){
            Instant now = Instant.now();
            while(!ventana.isEmpty() && Duration.between(ventana.peekFirst(),now).getSeconds()>=30){
                ventana.pollFirst();
            }
            if(ventana.size()>=2) return "⏳ Rate limit /farmacia/pedir";
            ventana.addLast(now);
        }

        Service7 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

public class Ejemplo7 {
    private static String usuario;
    private static String password;
    private static ApiGateway7 gw;

    public static void main(String[] args){
        LoginService7 ls = new LoginService7();
        gw = new ApiGateway7(ls);

        // === Registrar rutas ===
        gw.register("/farmacia/pedir",(data,user)->{
            String[] p = data.split("\\|");
            String prod = p[0];
            boolean conReceta = p.length>1 && p[1].toLowerCase().contains("true");

            if("Antibiótico X".equals(prod) && !conReceta) return "❌ Requiere receta médica.";

            int s = gw.stock.getOrDefault(prod,0);
            if(s<=0) return "⚠️ Agotado: " + prod;
            gw.stock.put(prod, s-1);
            return "✅ Pedido OK: " + prod + " | Quedan: " + (s-1);
        });

        gw.register("/farmacia/listar",(d,u)->{
            StringBuilder sb = new StringBuilder("📋 Stock actual:\n");
            gw.stock.forEach((k,v)-> sb.append(k).append(" -> ").append(v).append("\n"));
            return sb.toString();
        });

        gw.register("/farmacia/agregar",(data,user)->{
            String[] p = data.split("\\|");
            if(p.length<2) return "❌ Formato: Producto|Cantidad";
            int cant = Integer.parseInt(p[1]);
            gw.stock.put(p[0], gw.stock.getOrDefault(p[0],0)+cant);
            return "✅ Agregado "+cant+" unidades de "+p[0];
        });

        gw.register("/farmacia/eliminar",(data,user)->{
            if(gw.stock.remove(data)!=null) return "🗑️ Producto eliminado: "+data;
            return "❌ No encontrado.";
        });

        gw.register("/farmacia/buscar",(data,user)->{
            return gw.stock.containsKey(data) ?
                    "🔎 "+data+" disponible: "+gw.stock.get(data) :
                    "❌ Producto no encontrado.";
        });

        gw.register("/farmacia/receta",(data,user)->{
            return "📑 Receta validada para: "+data;
        });

        gw.register("/farmacia/limpiar",(d,u)->{
            gw.stock.clear();
            return "🧹 Stock limpiado.";
        });

        gw.register("/user/info",(d,u)-> "👤 Usuario logueado: "+u);
        gw.register("/user/logout",(d,u)-> "👋 Sesión cerrada para "+u);
        gw.register("/system/help",(d,u)-> "ℹ️ Opciones: A–J");

        // === Pantalla de login ===
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        Object[] fields = { "Usuario:", userField, "Contraseña:", passField };

        int option = JOptionPane.showConfirmDialog(null, fields, "Login", JOptionPane.OK_CANCEL_OPTION);
        if(option == JOptionPane.OK_OPTION){
            usuario = userField.getText();
            password = new String(passField.getPassword());
            if(!ls.login(usuario,password)){
                JOptionPane.showMessageDialog(null,"❌ Credenciales incorrectas.","Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else return;

        // === Menú principal ===
        String[] opciones = {
                "A. Pedir medicamento",
                "B. Listar stock",
                "C. Agregar stock",
                "D. Eliminar producto",
                "E. Buscar producto",
                "F. Validar receta",
                "G. Limpiar stock",
                "H. Ver info usuario",
                "I. Ayuda",
                "J. Cerrar sesión"
        };

        boolean activo = true;
        while(activo){
            String seleccion = (String) JOptionPane.showInputDialog(
                    null,"Selecciona una opción:","Farmacia - Menú",
                    JOptionPane.PLAIN_MESSAGE,null,opciones,opciones[0]);

            if(seleccion==null) break;

            String path="", data="";
            switch(seleccion.charAt(0)){
                case 'A' -> {
                    String prod = JOptionPane.showInputDialog("Producto:");
                    String receta = JOptionPane.showInputDialog("¿Tiene receta? (true/false):");
                    data = prod+"|"+receta;
                    path="/farmacia/pedir";
                }
                case 'B' -> path="/farmacia/listar";
                case 'C' -> {
                    data = JOptionPane.showInputDialog("Ingrese Producto|Cantidad:");
                    path="/farmacia/agregar";
                }
                case 'D' -> {
                    data = JOptionPane.showInputDialog("Producto a eliminar:");
                    path="/farmacia/eliminar";
                }
                case 'E' -> {
                    data = JOptionPane.showInputDialog("Producto a buscar:");
                    path="/farmacia/buscar";
                }
                case 'F' -> {
                    data = JOptionPane.showInputDialog("Producto para validar receta:");
                    path="/farmacia/receta";
                }
                case 'G' -> path="/farmacia/limpiar";
                case 'H' -> path="/user/info";
                case 'I' -> path="/system/help";
                case 'J' -> {
                    JOptionPane.showMessageDialog(null,gw.request(usuario,password,"/user/logout",""));
                    activo=false;
                }
            }

            if(!path.isEmpty() && activo){
                String resp = gw.request(usuario,password,path,data);
                JOptionPane.showMessageDialog(null, resp, "Resultado", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}
