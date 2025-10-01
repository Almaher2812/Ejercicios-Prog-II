import java.util.*;

/**
 * GIMNASIO: RBAC (admin crea clases; user se inscribe) + cupos por clase.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Control de acceso por rol (derivado del usuario)
 * - Manejo de cupos por recurso
 */
interface Service8 { String handle(String data, String user); }

/** Login simple */
class LoginService8 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway con cupos y verificación de rol */
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

public class Ejemplo8Gimnasio {
    public static void main(String[] args){
        LoginService8 ls = new LoginService8();
        ApiGateway8 gw = new ApiGateway8(ls);

        // Crear clase (solo admin) -> data: "Clase:cupos"
        gw.register("/gym/create",(data,user)->{
            if(!"admin".equals(user)) return "🚫 Solo admin puede crear clases.";
            String[] p = data.split(":");
            gw.cupos.put(p[0], Integer.parseInt(p[1]));
            return "✅ Clase creada: " + p[0] + " (" + p[1] + " cupos)";
        });

        // Inscribirse a clase -> decrementa cupo
        gw.register("/gym/inscribir",(data,user)->{
            int c = gw.cupos.getOrDefault(data,0);
            if(c<=0) return "Sin cupos en " + data;
            gw.cupos.put(data,c-1);
            return "🎫 " + user + " inscrito en " + data + " | Quedan: " + (c-1);
        });

        // Flujo de prueba
        System.out.println(gw.request("admin","1234","/gym/create","Crossfit:2"));
        System.out.println(gw.request("user","abcd","/gym/inscribir","Crossfit"));
        System.out.println(gw.request("user","abcd","/gym/inscribir","Crossfit"));
        System.out.println(gw.request("user","abcd","/gym/inscribir","Crossfit")); // sin cupo
    }
}
// EOF
