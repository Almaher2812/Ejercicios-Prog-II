import java.util.*;

/**
 * SUPERMERCADO: Carrito por usuario + subtotal + cupones + auditoría.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Estado por usuario (carrito)
 * - Precios, cupones, cálculo de subtotal/total
 * - Bitácora de acciones (auditoría)
 */
interface Service2 { String handle(String data, String user, Map<String,String> headers); }

/** Login simple en memoria */
class LoginService2 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
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
    private final Set<String> cupones = Set.of("DESC10","DESC20");
    // Auditoría (acciones realizadas)
    private final List<String> audit = new ArrayList<>();

    ApiGateway2(LoginService2 l){
        this.login = l;
        // Precios de ejemplo
        precios.put("Leche",3.5); precios.put("Pan",2.0);
        precios.put("Huevos",4.0); precios.put("Arroz",3.0);
    }

    void register(String p, Service2 s){ routes.put(p,s); }

    /** Valida login y rutea a la acción */
    String request(String u,String p,String path,String data, Map<String,String> headers){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service2 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u,headers);
    }

    // Getters para usar dentro de lambdas
    Map<String,List<String>> carts(){ return carritos; }
    Map<String,Double> price(){ return precios; }
    Set<String> coupons(){ return cupones; }
    List<String> log(){ return audit; }
}

public class Ejemplo2Supermercado {
    public static void main(String[] args){
        LoginService2 ls = new LoginService2();
        ApiGateway2 gw = new ApiGateway2(ls);

        // Agregar ítem al carrito del usuario
        gw.register("/super/add",(data,user,headers)->{
            gw.carts().computeIfAbsent(user,k->new ArrayList<>()).add(data);
            gw.log().add("ADD "+user+" -> "+data);
            return "✅ Agregado al carrito: " + data;
        });

        // Checkout: calcula subtotal, aplica cupón (si existe) y retorna total
        gw.register("/super/checkout",(data,user,headers)->{
            List<String> items = gw.carts().getOrDefault(user, List.of());
            double subtotal = 0;
            for(String it: items) subtotal += gw.price().getOrDefault(it,1.0);
            // Cupón: DESC10 => 10%, DESC20 => 20%
            double desc = gw.coupons().contains(data) ? (data.equals("DESC20")?0.20:0.10) : 0.0;
            double total = Math.round(subtotal*(1-desc)*100.0)/100.0;
            gw.log().add("CHECKOUT "+user+" subtotal="+subtotal+" cupon="+data+" total="+total);
            return "🧾 Items="+items+" | Subtotal="+subtotal+" | Cupón="+(desc>0?data:"N/A")+" | Total="+total;
        });

        // Pruebas
        Map<String,String> H = Map.of(); // headers vacíos
        System.out.println(gw.request("admin","1234","/super/add","Leche",H));
        System.out.println(gw.request("admin","1234","/super/add","Pan",H));
        System.out.println(gw.request("admin","1234","/super/checkout","DESC10",H));
    }
}
// EOF
