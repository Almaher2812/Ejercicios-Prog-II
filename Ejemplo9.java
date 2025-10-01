import java.util.*;
import java.time.*;

/**
 * HOTEL: Circuit Breaker frente a pasarela de pagos inestable.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Patrón de resiliencia Circuit Breaker con estados CLOSED/OPEN/HALF_OPEN
 * - Política: 3 fallos cierran circuito 5s; en HALF_OPEN, un fallo reabre
 */
interface Service9 { String handle(String data, String user); }

/** Login simple */
class LoginService9 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway simple; breaker se maneja en el main para visibilidad */
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

public class Ejemplo9Hotel {
    // Estados del breaker
    enum State { CLOSED, OPEN, HALF_OPEN }

    public static void main(String[] args){
        LoginService9 ls = new LoginService9();
        ApiGateway9 gw = new ApiGateway9(ls);

        Random rnd = new Random();

        // Estructura del breaker (contadores y tiempos)
        class Breaker {
            State state = State.CLOSED;
            int fails = 0;
            Instant openedAt = null;
        }
        Breaker cb = new Breaker();

        // Endpoint de pago que consulta un "servicio inestable"
        gw.register("/hotel/pagar",(data,user)->{
            // Si está OPEN, esperamos 5s antes de permitir intento (pasar a HALF_OPEN)
            if(cb.state==State.OPEN){
                if(Duration.between(cb.openedAt,Instant.now()).getSeconds()>=5){
                    cb.state=State.HALF_OPEN; // probamos una solicitud
                }else{
                    return "⛔ Circuito ABIERTO. Intente luego.";
                }
            }

            // Simulamos 25% de éxito
            boolean ok = rnd.nextInt(4)==0;
            if(ok){
                // Éxito: cerramos circuito y limpiamos fallos
                cb.fails=0; cb.state=State.CLOSED;
                return "💳 Pago aprobado para " + data + " ["+user+"]";
            }else{
                // Falla: contamos y revisamos transición de estado
                cb.fails++;
                if(cb.state==State.HALF_OPEN || cb.fails>=3){
                    cb.state=State.OPEN; cb.openedAt=Instant.now();
                    return "❌ Pago rechazado. Breaker ABIERTO";
                }
                return "❌ Pago rechazado. Fails=" + cb.fails;
            }
        });

        // Varias llamadas para observar cambios de estado del breaker
        for(int i=0;i<7;i++){
            System.out.println(gw.request("user","abcd","/hotel/pagar","Reserva Suite #777"));
        }
    }
}
// EOF
