import javax.swing.*;
import java.awt.*;
import java.util.*;

// === Servicios ===
interface Service6 { String handle(String data, String user); }

class LoginService6 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

class ApiGateway6 {
    private final LoginService6 login;
    private final Map<String,Service6> routes = new HashMap<>();
    final Map<String,String> db = new HashMap<>();
    final Map<String,String> cache = new HashMap<>();

    ApiGateway6(LoginService6 l){
        this.login=l;
        db.put("978-0132350884","Clean Code - Robert C. Martin");
        db.put("978-0201633610","Design Patterns - GoF");
        db.put("978-0134685991","Effective Java - Joshua Bloch");
    }
    void register(String p, Service6 s){ routes.put(p,s); }
    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service6 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

// === IU con Swing ===
public class Ejemplo6 {
    private static String usuario;
    private static String password;
    private static ApiGateway6 gw;

    public static void main(String[] args){
        LoginService6 ls = new LoginService6();
        gw = new ApiGateway6(ls);

        // Registrar rutas
        gw.register("/libro/get",(data,user)->{
            String v = gw.cache.get(data);
            if(v!=null) return "📖 " + v + " (cache)";
            v = gw.db.getOrDefault(data,"No encontrado");
            gw.cache.put(data,v);
            return "📖 " + v + " (db)";
        });
        gw.register("/libro/addStock",(data,user)->{
            gw.cache.remove(data);
            return "✅ Stock actualizado para "+data+" (cache invalidado)";
        });
        gw.register("/libro/add",(data,user)->{
            String[] parts = data.split(";",2);
            if(parts.length<2) return "❌ Formato: ISBN;Titulo";
            gw.db.put(parts[0],parts[1]);
            gw.cache.remove(parts[0]);
            return "📚 Libro agregado: "+parts[1];
        });
        gw.register("/libro/remove",(data,user)->{
            String removed = gw.db.remove(data);
            gw.cache.remove(data);
            return removed!=null ? "🗑️ Libro eliminado: "+removed : "❌ No encontrado";
        });
        gw.register("/libro/list",(d,u)->{
            return "📚 Libros en DB:\n" + String.join("\n", gw.db.values());
        });
        gw.register("/libro/listCache",(d,u)->{
            return gw.cache.isEmpty()? "⚠️ Cache vacío" :
                    "📦 Libros en cache:\n" + String.join("\n", gw.cache.values());
        });
        gw.register("/cache/clear",(d,u)->{
            gw.cache.clear();
            return "🧹 Cache limpiada";
        });
        gw.register("/user/info",(d,u)-> "👤 Usuario: "+u+" (logueado)");
        gw.register("/user/logout",(d,u)-> "👋 Sesión finalizada para "+u);
        gw.register("/system/help",(d,u)-> "ℹ️ Opciones disponibles: A–J");

        // Pantalla de login
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        Object[] fields = {
                "Usuario:", userField,
                "Contraseña:", passField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Login", JOptionPane.OK_CANCEL_OPTION);
        if(option == JOptionPane.OK_OPTION){
            usuario = userField.getText();
            password = new String(passField.getPassword());
            if(!ls.login(usuario,password)){
                JOptionPane.showMessageDialog(null,"❌ Credenciales incorrectas.","Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            return;
        }

        // Menú principal
        String[] opciones = {
                "A. Consultar libro",
                "B. Actualizar stock",
                "C. Agregar libro",
                "D. Eliminar libro",
                "E. Listar libros (DB)",
                "F. Listar libros (Cache)",
                "G. Limpiar cache",
                "H. Ver info usuario",
                "I. Ayuda",
                "J. Cerrar sesión"
        };

        boolean activo = true;
        while(activo){
            String seleccion = (String) JOptionPane.showInputDialog(
                    null,
                    "Selecciona una opción:",
                    "Menú Principal",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );

            if(seleccion==null) break;

            String path="", data="";
            switch(seleccion.charAt(0)){
                case 'A' -> {
                    data = JOptionPane.showInputDialog("Ingrese ISBN:");
                    path="/libro/get";
                }
                case 'B' -> {
                    data = JOptionPane.showInputDialog("Ingrese ISBN:");
                    path="/libro/addStock";
                }
                case 'C' -> {
                    data = JOptionPane.showInputDialog("Ingrese ISBN;Titulo:");
                    path="/libro/add";
                }
                case 'D' -> {
                    data = JOptionPane.showInputDialog("Ingrese ISBN a eliminar:");
                    path="/libro/remove";
                }
                case 'E' -> path="/libro/list";
                case 'F' -> path="/libro/listCache";
                case 'G' -> path="/cache/clear";
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
