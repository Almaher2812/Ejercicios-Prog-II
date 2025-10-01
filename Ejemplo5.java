import java.util.*;

/**
 * RESTAURANTE: Canary routing (10% hacia versión B) entre dos versiones del servicio.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Envío de una fracción de peticiones a una nueva versión (experimento controlado)
 */
interface Service5 { String handle(String data, String user); }

/** Login simple */
class LoginService5 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway con canary routing */
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

public class Ejemplo5Restaurante {
    public static void main(String[] args){
        LoginService5 ls = new LoginService5();
        ApiGateway5 gw = new ApiGateway5(ls);
        Random rnd = new Random();

        // vA: menú clásico, vB: menú nuevo
        Service5 vA = (data,user)->"🍝 Menú A: Pasta/Pizza/Ensalada -> Orden: " + data + " ["+user+"]";
        Service5 vB = (data,user)->"🍣 Menú B NUEVO: Sushi/Ramen/Poke -> Orden: " + data + " ["+user+"]";

        // Canary: 10% de requests caen en vB
        gw.register("/rest/orden",(data,user)-> (rnd.nextInt(10)==0 ? vB : vA).handle(data,user));

        // Varias órdenes para ver la mezcla A/B
        for(int i=1;i<=6;i++){
            System.out.println(gw.request("user","abcd","/rest/orden","Plato #"+i));
        }
    }
}
// EOF
