import java.util.*;
import java.time.*;

/**
 * TRANSPORTE: Idempotencia (X-Idempotency-Key) + tarifa dinámica por hora pico.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Prevención de compras duplicadas (misma key -> misma respuesta)
 * - Cálculo de tarifa según horario pico
 */
interface Service3 { String handle(String data, String user, Map<String,String> headers); }

/** Login simple */
class LoginService3 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway con tracking de claves idempotentes */
class ApiGateway3 {
    private final LoginService3 login;
    private final Map<String,Service3> routes = new HashMap<>();
    private final Set<String> idemUsed = new HashSet<>(); // claves ya usadas

    ApiGateway3(LoginService3 l){ this.login=l; }
    void register(String p, Service3 s){ routes.put(p,s); }

    String request(String u,String p,String path,String data, Map<String,String> headers){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service3 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u,headers);
    }

    /** Marca clave de idempotencia y retorna true si ya estaba usada */
    boolean markIdem(String key){
        if(key==null || key.isBlank()) return false;
        if(idemUsed.contains(key)) return true;
        idemUsed.add(key); return false;
    }
}

public class Ejemplo3Transporte {
    public static void main(String[] args){
        LoginService3 ls = new LoginService3();
        ApiGateway3 gw = new ApiGateway3(ls);

        // Comprar pasaje (requiere header X-Idempotency-Key)
        gw.register("/bus/pasaje",(data,user,headers)->{
            String key = headers.get("X-Idempotency-Key");
            if(key==null) return "❌ Falta X-Idempotency-Key";
            if(gw.markIdem(key)) return "✅ Compra ya registrada (idempotente).";

            // Tarifa dinámica: hora pico = 6-9 y 16-19
            LocalTime now = LocalTime.now();
            boolean pico = (now.getHour()>=6 && now.getHour()<=9) || (now.getHour()>=16 && now.getHour()<=19);
            double base = 3000, total = pico? base*1.3 : base;
            return "🎫 Ruta=" + data + " | Tarifa=" + total + (pico?" (pico)":"");
        });

        // Prueba con la misma clave dos veces
        Map<String,String> H = new HashMap<>();
        H.put("X-Idempotency-Key","ABC-123");
        System.out.println(gw.request("user","abcd","/bus/pasaje","Ruta 80",H));
        System.out.println(gw.request("user","abcd","/bus/pasaje","Ruta 80",H)); // duplicada
    }
}
// EOF
