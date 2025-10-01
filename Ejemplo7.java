import java.util.*;
import java.time.*;

/**
 * FARMACIA: Validación receta p/ controlados + stock decreciente + rate-limit endpoint.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Validación de dominio (receta obligatoria para cierto producto)
 * - Control de stock
 * - Rate-limit específico del endpoint /farmacia/pedir (2 req / 30s)
 */
interface Service7 { String handle(String data, String user); }

/** Login simple */
class LoginService7 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway con stock y rate-limit por endpoint */
class ApiGateway7 {
    private final LoginService7 login;
    private final Map<String,Service7> routes = new HashMap<>();
    final Map<String,Integer> stock = new HashMap<>();
    // Ventana global simple para este endpoint (para simplificar ejemplo)
    final Deque<Instant> ventana = new ArrayDeque<>();

    ApiGateway7(LoginService7 l){
        this.login=l;
        stock.put("Paracetamol 500",2);
        stock.put("Antibiótico X",1); // producto controlado
    }

    void register(String p, Service7 s){ routes.put(p,s); }

    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";

        // Rate-limit solo para /farmacia/pedir: máximo 2 req cada 30s
        if("/farmacia/pedir".equals(path)){
            Instant now=Instant.now();
            while(!ventana.isEmpty() && Duration.between(ventana.peekFirst(),now).getSeconds()>=30) ventana.pollFirst();
            if(ventana.size()>=2) return "⏳ Rate limit /farmacia/pedir";
            ventana.addLast(now);
        }

        Service7 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

public class Ejemplo7Farmacia {
    public static void main(String[] args){
        LoginService7 ls = new LoginService7();
        ApiGateway7 gw = new ApiGateway7(ls);

        // data: "Producto|requiereReceta:true/false"
        gw.register("/farmacia/pedir",(data,user)->{
            String[] p = data.split("\\|"); // separa producto y flag de receta
            String prod = p[0];
            boolean conReceta = p.length>1 && p[1].toLowerCase().contains("true");

            // Regla de negocio: Antibiótico X requiere receta
            if("Antibiótico X".equals(prod) && !conReceta) return "❌ Requiere receta médica.";

            int s = gw.stock.getOrDefault(prod,0);
            if(s<=0) return "Agotado: " + prod;
            gw.stock.put(prod, s-1); // decrementa stock
            return "✅ Pedido OK: " + prod + " | Quedan: " + (s-1);
        });

        // Pruebas
        System.out.println(gw.request("admin","1234","/farmacia/pedir","Paracetamol 500|requiereReceta:false"));
        System.out.println(gw.request("admin","1234","/farmacia/pedir","Antibiótico X|requiereReceta:true"));
        System.out.println(gw.request("admin","1234","/farmacia/pedir","Antibiótico X|requiereReceta:true")); // agotado o rate limit
    }
}
// EOF
