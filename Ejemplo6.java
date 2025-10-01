import java.util.*;

/**
 * LIBRERÍA: Cache de lectura (cache-aside) + invalidación al actualizar stock.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Cache en memoria para lecturas repetidas
 * - Invalidation cuando se actualiza el recurso
 */
interface Service6 { String handle(String data, String user); }

/** Login simple */
class LoginService6 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway con "db" simulada + cache */
class ApiGateway6 {
    private final LoginService6 login;
    private final Map<String,Service6> routes = new HashMap<>();
    final Map<String,String> db = new HashMap<>();    // Simula base de datos
    final Map<String,String> cache = new HashMap<>(); // Cache en memoria

    ApiGateway6(LoginService6 l){
        this.login=l;
        db.put("978-0132350884","Clean Code - Robert C. Martin");
        db.put("978-0201633610","Design Patterns - GoF");
    }
    void register(String p, Service6 s){ routes.put(p,s); }
    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service6 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

public class Ejemplo6Libreria {
    public static void main(String[] args){
        LoginService6 ls = new LoginService6();
        ApiGateway6 gw = new ApiGateway6(ls);

        // Lectura con cache-aside
        gw.register("/libro/get",(data,user)->{
            String v = gw.cache.get(data);
            if(v!=null) return "📖 " + v + " (cache)";
            v = gw.db.getOrDefault(data,"No encontrado");
            gw.cache.put(data,v);
            return "📖 " + v + " (db)";
        });

        // Actualización de stock => invalidar cache del ISBN
        gw.register("/libro/addStock",(data,user)->{
            gw.cache.remove(data);
            return "✅ Stock actualizado para "+data+" (cache invalidado)";
        });

        // Flujo de prueba
        System.out.println(gw.request("user","abcd","/libro/get","978-0132350884"));
        System.out.println(gw.request("user","abcd","/libro/get","978-0132350884")); // cache
        System.out.println(gw.request("user","abcd","/libro/addStock","978-0132350884"));
        System.out.println(gw.request("user","abcd","/libro/get","978-0132350884")); // repoblado
    }
}
// EOF
