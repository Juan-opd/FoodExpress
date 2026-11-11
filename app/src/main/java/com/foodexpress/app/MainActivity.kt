package com.foodexpress.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val Orange = Color(0xFFFF6B35)
private val OrangeDark = Color(0xFFE95321)
private val Cream = Color(0xFFFFF8F3)
private val Dark = Color(0xFF202124)
private val Gray = Color(0xFF6B6B6B)
private val SoftOrange = Color(0xFFFFEEE5)
private val SoftGreen = Color(0xFFE9F8EF)
private val Green = Color(0xFF219653)

private fun money(value: Double) = String.format(Locale.US, "$%.2f", value)

private data class Food(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val emoji: String,
    val category: String,
    val rating: Double,
    val time: String,
    val popular: Boolean = false
)

private data class CartItem(val food: Food, val quantity: Int)
private data class OrderItem(val name: String, val quantity: Int, val price: Double, val emoji: String)
private data class Order(
    val number: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val delivery: Double,
    val discount: Double,
    val total: Double,
    val address: String,
    val payment: String,
    val date: String
)

private val foods = listOf(
    Food("burger", "Hamburguesa Clásica", "Carne, queso, lechuga, tomate y salsa especial", 5.99, "🍔", "Hamburguesas", 4.9, "20-30 min", true),
    Food("pepperoni", "Pizza Pepperoni", "Mozzarella, pepperoni y salsa de tomate", 8.50, "🍕", "Pizza", 4.8, "25-35 min", true),
    Food("tacos", "Tacos Mexicanos", "Tres tacos con carne, queso, cebolla y vegetales", 6.25, "🌮", "Mexicana", 4.9, "20-30 min", true),
    Food("crispy", "Pollo Crispy", "Pollo crujiente, papas fritas y salsa de la casa", 7.75, "🍗", "Pollo", 4.7, "25-35 min"),
    Food("sushi", "Sushi Mix", "Selección de rolls con ingredientes frescos", 10.99, "🍣", "Sushi", 4.8, "30-40 min"),
    Food("salad", "Ensalada Fresca", "Lechuga, tomate, pepino, aguacate y aderezo", 5.25, "🥗", "Saludable", 4.6, "15-25 min"),
    Food("burrito", "Burrito Especial", "Tortilla, carne, frijoles, arroz, queso y salsa", 7.25, "🌯", "Mexicana", 4.7, "20-30 min"),
    Food("fries", "Papas Cargadas", "Papas crujientes con queso y salsa especial", 4.25, "🍟", "Hamburguesas", 4.8, "15-20 min"),
    Food("pasta", "Pasta Alfredo", "Pasta cremosa con pollo y queso parmesano", 8.95, "🍝", "Pasta", 4.7, "25-35 min"),
    Food("cake", "Cheesecake", "Postre cremoso con salsa de frutos rojos", 4.75, "🍰", "Postres", 4.9, "15-20 min")
)


private class FoodDatabase(context: Context) : android.database.sqlite.SQLiteOpenHelper(context, "foodexpress.db", null, 1) {
    override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
        db.execSQL("CREATE TABLE users (email TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, password_hash TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)")
        db.execSQL("CREATE TABLE products (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, price REAL NOT NULL, emoji TEXT NOT NULL, category TEXT NOT NULL, rating REAL NOT NULL, delivery_time TEXT NOT NULL, popular INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE favorites (user_email TEXT NOT NULL, product_id TEXT NOT NULL, PRIMARY KEY(user_email, product_id), FOREIGN KEY(user_email) REFERENCES users(email), FOREIGN KEY(product_id) REFERENCES products(id))")
        db.execSQL("CREATE TABLE cart_items (user_email TEXT NOT NULL, product_id TEXT NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY(user_email, product_id))")
        db.execSQL("CREATE TABLE orders (number TEXT PRIMARY KEY NOT NULL, user_email TEXT NOT NULL, subtotal REAL NOT NULL, delivery REAL NOT NULL, discount REAL NOT NULL, total REAL NOT NULL, address TEXT NOT NULL, payment TEXT NOT NULL, date TEXT NOT NULL)")
        db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_number TEXT NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL, price REAL NOT NULL, emoji TEXT NOT NULL, FOREIGN KEY(order_number) REFERENCES orders(number) ON DELETE CASCADE)")
        listOf("Hamburguesas", "Pizza", "Mexicana", "Pollo", "Sushi", "Saludable", "Pasta", "Postres").forEach { name ->
            db.execSQL("INSERT OR IGNORE INTO categories(name) VALUES(?)", arrayOf(name))
        }
    }
    override fun onUpgrade(db: android.database.sqlite.SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun seedFoods(items: List<Food>) {
        val db = writableDatabase
        items.forEach { f ->
            val values = android.content.ContentValues().apply {
                put("id", f.id); put("name", f.name); put("description", f.description); put("price", f.price)
                put("emoji", f.emoji); put("category", f.category); put("rating", f.rating); put("delivery_time", f.time); put("popular", if (f.popular) 1 else 0)
            }
            db.insertWithOnConflict("products", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun register(name: String, email: String, passwordHash: String): Boolean {
        val values = android.content.ContentValues().apply { put("email", email); put("name", name); put("password_hash", passwordHash); put("created_at", System.currentTimeMillis()) }
        return writableDatabase.insertWithOnConflict("users", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }
    fun userExists(email: String): Boolean = readableDatabase.rawQuery("SELECT 1 FROM users WHERE email=?", arrayOf(email)).use { it.moveToFirst() }
    fun passwordHash(email: String): String? = readableDatabase.rawQuery("SELECT password_hash FROM users WHERE email=?", arrayOf(email)).use { if (it.moveToFirst()) it.getString(0) else null }
    fun userName(email: String): String = readableDatabase.rawQuery("SELECT name FROM users WHERE email=?", arrayOf(email)).use { if (it.moveToFirst()) it.getString(0) else "Usuario" }

    fun favorites(email: String): Set<String> = readableDatabase.rawQuery("SELECT product_id FROM favorites WHERE user_email=?", arrayOf(email)).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }
    fun saveFavorites(email: String, ids: Set<String>) {
        val db = writableDatabase; db.beginTransaction()
        try {
            db.delete("favorites", "user_email=?", arrayOf(email))
            ids.forEach { id -> db.execSQL("INSERT OR IGNORE INTO favorites(user_email,product_id) VALUES(?,?)", arrayOf(email,id)) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun saveOrders(email: String, orders: List<Order>) {
        val db = writableDatabase; db.beginTransaction()
        try {
            db.delete("orders", "user_email=?", arrayOf(email))
            orders.forEach { o ->
                val v = android.content.ContentValues().apply {
                    put("number",o.number); put("user_email",email); put("subtotal",o.subtotal); put("delivery",o.delivery)
                    put("discount",o.discount); put("total",o.total); put("address",o.address); put("payment",o.payment); put("date",o.date)
                }
                db.insertWithOnConflict("orders",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                db.delete("order_items","order_number=?", arrayOf(o.number))
                o.items.forEach { item ->
                    val iv=android.content.ContentValues().apply { put("order_number",o.number); put("name",item.name); put("quantity",item.quantity); put("price",item.price); put("emoji",item.emoji) }
                    db.insert("order_items",null,iv)
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun loadOrders(email: String): List<Order> {
        val result= mutableListOf<Order>()
        readableDatabase.rawQuery("SELECT number,subtotal,delivery,discount,total,address,payment,date FROM orders WHERE user_email=? ORDER BY rowid DESC",arrayOf(email)).use { c ->
            while(c.moveToNext()) {
                val number=c.getString(0); val items= mutableListOf<OrderItem>()
                readableDatabase.rawQuery("SELECT name,quantity,price,emoji FROM order_items WHERE order_number=?",arrayOf(number)).use { ic ->
                    while(ic.moveToNext()) items.add(OrderItem(ic.getString(0),ic.getInt(1),ic.getDouble(2),ic.getString(3)))
                }
                result.add(Order(number,items,c.getDouble(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getString(5),c.getString(6),c.getString(7)))
            }
        }
        return result
    }
}

private class FoodPrefs(private val db: FoodDatabase, private val email: () -> String) {
    fun loadFavorites() = db.favorites(email())
    fun saveFavorites(ids: Set<String>) = db.saveFavorites(email(), ids)
    fun loadOrders() = db.loadOrders(email())
    fun saveOrders(orders: List<Order>) = db.saveOrders(email(), orders)
}

private class AuthStore(context: Context, private val db: FoodDatabase) {
    private val prefs = context.getSharedPreferences("foodexpress_auth", Context.MODE_PRIVATE)
    fun currentUser(): String? = prefs.getString("current_user", null)
    fun logout() { prefs.edit().remove("current_user").apply() }
    fun register(name: String, email: String, password: String): Boolean {
        val key=email.trim().lowercase(Locale.ROOT)
        if (key.isBlank() || db.userExists(key)) return false
        val ok=db.register(name.trim(),key,hash(password))
        if(ok) prefs.edit().putString("current_user",key).apply()
        return ok
    }
    fun login(email: String, password: String): Boolean {
        val key=email.trim().lowercase(Locale.ROOT)
        val stored=db.passwordHash(key) ?: return false
        if(stored != hash(password)) return false
        prefs.edit().putString("current_user",key).apply()
        return true
    }
    fun userName(): String = currentUser()?.let { db.userName(it) } ?: "Usuario"
    private fun hash(value: String): String {
        val digest=MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = FoodDatabase(this)
        database.seedFoods(foods)
        val auth = AuthStore(this, database)
        setContent { FoodExpressApp(FoodPrefs(database) { auth.currentUser() ?: "" }, auth) }
    }
}

@Composable
private fun FoodExpressApp(store: FoodPrefs, auth: AuthStore) {
    var loggedIn by remember { mutableStateOf(auth.currentUser() != null) }
    var authMode by remember { mutableStateOf("login") }
    if (!loggedIn) {
        if (authMode == "login") {
            LoginScreen(
                onLogin = { email, password ->
                    if (auth.login(email, password)) { loggedIn = true; true } else false
                },
                onRegister = { authMode = "register" }
            )
        } else {
            RegisterScreen(
                onRegister = { name, email, password ->
                    if (auth.register(name, email, password)) { loggedIn = true; true } else false
                },
                onLogin = { authMode = "login" }
            )
        }
        return
    }

    var screen by remember { mutableStateOf("home") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var cart by remember { mutableStateOf(emptyList<CartItem>()) }
    var favorites by remember { mutableStateOf(store.loadFavorites()) }
    var orders by remember { mutableStateOf(store.loadOrders()) }
    var snackbar by remember { mutableStateOf<String?>(null) }

    fun showMessage(message: String) { snackbar = message }
    fun toggleFavorite(food: Food) {
        favorites = if (food.id in favorites) favorites - food.id else favorites + food.id
        store.saveFavorites(favorites)
        showMessage(if (food.id in favorites) "Agregado a favoritos ❤️" else "Quitado de favoritos")
    }
    fun addToCart(food: Food, quantity: Int = 1) {
        val existing = cart.find { it.food.id == food.id }
        cart = if (existing == null) cart + CartItem(food, max(1, quantity))
        else cart.map { if (it.food.id == food.id) it.copy(quantity = it.quantity + quantity) else it }
        showMessage("${food.name} agregado al carrito")
    }
    fun decrease(food: Food) {
        cart = cart.mapNotNull { item ->
            if (item.food.id == food.id) if (item.quantity > 1) item.copy(quantity = item.quantity - 1) else null else item
        }
    }

    val totalItems = cart.sumOf { it.quantity }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbar) {
        snackbar?.let { snackbarHostState.showSnackbar(it); snackbar = null }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Orange,
            onPrimary = Color.White,
            secondary = OrangeDark,
            background = Cream,
            surface = Color.White,
            onBackground = Dark,
            onSurface = Dark
        )
    ) {
        Scaffold(
            containerColor = Cream,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (selectedFood == null && screen != "checkout") {
                    NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
                        NavigationBarItem(
                            selected = screen == "home",
                            onClick = { screen = "home" },
                            icon = { Icon(Icons.Default.Home, "Inicio") },
                            label = { Text("Inicio") }
                        )
                        NavigationBarItem(
                            selected = screen == "favorites",
                            onClick = { screen = "favorites" },
                            icon = { Icon(Icons.Default.Favorite, "Favoritos") },
                            label = { Text("Favoritos") }
                        )
                        NavigationBarItem(
                            selected = screen == "orders",
                            onClick = { screen = "orders" },
                            icon = { Icon(Icons.Default.ReceiptLong, "Pedidos") },
                            label = { Text("Pedidos") }
                        )
                        NavigationBarItem(
                            selected = screen == "cart",
                            onClick = { screen = "cart" },
                            icon = {
                                BadgedBox({ if (totalItems > 0) Badge { Text(totalItems.toString()) } }) {
                                    Icon(Icons.Default.ShoppingCart, "Carrito")
                                }
                            },
                            label = { Text("Carrito") }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                when {
                    selectedFood != null -> DetailScreen(
                        modifier = Modifier.padding(padding), food = selectedFood!!,
                        favorite = selectedFood!!.id in favorites,
                        onBack = { selectedFood = null },
                        onFavorite = { toggleFavorite(selectedFood!!) },
                        onAdd = { quantity -> addToCart(selectedFood!!, quantity); selectedFood = null; screen = "cart" }
                    )
                    screen == "home" -> HomeScreen(
                        Modifier.padding(padding), selectedCategory, favorites,
                        onCategory = { selectedCategory = it }, onAdd = { addToCart(it) },
                        onFood = { selectedFood = it }, onFavorite = { toggleFavorite(it) }, onCart = { screen = "cart" }, onProfile = { screen = "profile" }, userName = auth.userName()
                    )
                    screen == "favorites" -> FavoritesScreen(
                        Modifier.padding(padding), favorites, onFood = { selectedFood = it },
                        onFavorite = { toggleFavorite(it) }, onAdd = { addToCart(it) }
                    )
                    screen == "cart" -> CartScreen(
                        Modifier.padding(padding), cart, onIncrease = { addToCart(it) }, onDecrease = { decrease(it) },
                        onClear = { cart = emptyList() }, onCheckout = { screen = "checkout" }, onContinue = { screen = "home" }
                    )
                    screen == "checkout" -> CheckoutScreen(
                        Modifier.padding(padding), cart, onBack = { screen = "cart" },
                        onDone = { address, payment, subtotal, delivery, discount, total ->
                            val order = Order(
                                number = "FE-${(100000..999999).random()}",
                                items = cart.map { OrderItem(it.food.name, it.quantity, it.food.price, it.food.emoji) },
                                subtotal = subtotal, delivery = delivery, discount = discount, total = total,
                                address = address, payment = payment,
                                date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                            )
                            orders = listOf(order) + orders
                            store.saveOrders(orders)
                            cart = emptyList()
                            screen = "orders"
                            showMessage("¡Pedido realizado con éxito! 🎉")
                        }
                    )
                    screen == "profile" -> ProfileScreen(
                        Modifier.padding(padding), auth.userName(), auth.currentUser() ?: "",
                        onBack = { screen = "home" },
                        onLogout = { auth.logout(); loggedIn = false; authMode = "login" }
                    )
                    else -> OrdersScreen(Modifier.padding(padding), orders)
                }
            }
        }
    }
}


@Composable
private fun LoginScreen(onLogin: (String, String) -> Boolean, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Surface(color = Cream, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(Orange), contentAlignment = Alignment.Center) { Text("🍔", fontSize = 52.sp) }
            Spacer(Modifier.height(16.dp))
            Text("FoodExpress", color = Orange, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text("Tu comida favorita, a un toque de distancia", color = Gray, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Iniciar sesión", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Entra a tu cuenta para continuar", color = Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(email, { email = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Correo electrónico") }, leadingIcon = { Icon(Icons.Default.Email, null) }, shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(11.dp))
                    OutlinedTextField(password, { password = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            when {
                                email.isBlank() || !email.contains("@") -> error = "Escribe un correo válido."
                                password.length < 6 -> error = "La contraseña debe tener al menos 6 caracteres."
                                !onLogin(email, password) -> error = "Correo o contraseña incorrectos."
                            }
                        }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)
                    ) { Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("¿No tienes cuenta? Crear una") }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Tus datos se guardan localmente en este dispositivo.", color = Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RegisterScreen(onRegister: (String, String, String) -> Boolean, onLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Surface(color = Cream, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Text("Crear cuenta", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Regístrate para pedir tus platos favoritos", color = Gray, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(name, { name = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Nombre") }, leadingIcon = { Icon(Icons.Default.Person, null) }, shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(email, { email = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Correo electrónico") }, leadingIcon = { Icon(Icons.Default.Email, null) }, shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(password, { password = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(confirm, { confirm = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Confirmar contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(17.dp))
                    Button(onClick = {
                        when {
                            name.trim().length < 2 -> error = "Escribe tu nombre."
                            !email.contains("@") -> error = "Escribe un correo válido."
                            password.length < 6 -> error = "La contraseña debe tener al menos 6 caracteres."
                            password != confirm -> error = "Las contraseñas no coinciden."
                            !onRegister(name, email, password) -> error = "Ya existe una cuenta con ese correo."
                        }
                    }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("Crear mi cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Ya tengo una cuenta") }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(modifier: Modifier, name: String, email: String, onBack: () -> Unit, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            Text("Mi cuenta", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Orange), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Text("👤", fontSize = 34.sp) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(name, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text(email, color = Color.White.copy(.85f), fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        AccountOption(Icons.Default.Person, "Datos personales", "Tu nombre y correo")
        AccountOption(Icons.Default.LocationOn, "Direcciones", "Gestiona tus direcciones de entrega")
        AccountOption(Icons.Default.CreditCard, "Métodos de pago", "Configura tus opciones de pago")
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
            Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión")
        }
    }
}

@Composable
private fun AccountOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Orange)
            Spacer(Modifier.width(14.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Gray, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier, selectedCategory: String, favorites: Set<String>,
    onCategory: (String) -> Unit, onAdd: (Food) -> Unit, onFood: (Food) -> Unit,
    onFavorite: (Food) -> Unit, onCart: () -> Unit, onProfile: () -> Unit, userName: String
) {
    val categories = listOf("Todos", "Hamburguesas", "Pizza", "Mexicana", "Pollo", "Sushi", "Pasta", "Postres", "Saludable")
    var search by remember { mutableStateOf("") }
    val visible = foods.filter {
        (selectedCategory == "Todos" || it.category == selectedCategory) &&
            (search.isBlank() || it.name.contains(search, true) || it.description.contains(search, true))
    }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FoodExpress", color = Orange, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Hola, ${userName.substringBefore(' ').ifBlank { userName }} 👋", color = Dark, fontSize = 15.sp)
                }
                IconButton(onClick = onProfile) {
                    Icon(Icons.Default.AccountCircle, "Mi cuenta", tint = Orange, modifier = Modifier.size(30.dp))
                }
                IconButton(onClick = onCart) {
                    Icon(Icons.Default.ShoppingCart, "Carrito", tint = Orange, modifier = Modifier.size(29.dp))
                }
            }
            Text("¿Qué se te antoja hoy?", color = Gray, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (search.isNotEmpty()) IconButton({ search = "" }) { Icon(Icons.Default.Close, null) } },
                placeholder = { Text("Busca pizza, tacos, hamburguesas...") }, shape = RoundedCornerShape(17.dp)
            )
            Spacer(Modifier.height(16.dp))
            PromoBanner()
            Spacer(Modifier.height(20.dp))
            Text("Categorías", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category -> FilterChip(selectedCategory == category, { onCategory(category) }, label = { Text(category) }) }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (search.isBlank()) "Lo más popular" else "Resultados", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${visible.size} opciones", color = Gray, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (visible.isEmpty()) {
            item { EmptyState("🔎", "No encontramos ese platillo", "Prueba con otro nombre o categoría.") }
        } else {
            items(visible) { food ->
                FoodCard(food, food.id in favorites, onAdd, onFood, onFavorite)
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun PromoBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = Orange), shape = RoundedCornerShape(23.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("OFERTA DE HOY", color = Color.White.copy(.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("20% de descuento", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Usa el código BIENVENIDO", color = Color.White.copy(.9f), fontSize = 13.sp)
            }
            Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(.18f)), contentAlignment = Alignment.Center) {
                Text("🍔", fontSize = 44.sp)
            }
        }
    }
}

@Composable
private fun FoodCard(food: Food, favorite: Boolean, onAdd: (Food) -> Unit, onFood: (Food) -> Unit, onFavorite: (Food) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onFood(food) },
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(82.dp).clip(RoundedCornerShape(17.dp)).background(SoftOrange), contentAlignment = Alignment.Center) {
                Text(food.emoji, fontSize = 42.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(food.description, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Gray, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ${food.rating}", color = OrangeDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("  •  ${food.time}", color = Gray, fontSize = 12.sp)
                }
                Text(money(food.price), color = Orange, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onFavorite(food) }) {
                    Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (favorite) Orange else Gray)
                }
                IconButton(onClick = { onAdd(food) }) {
                    Icon(Icons.Default.AddCircle, "Agregar", tint = Orange, modifier = Modifier.size(31.dp))
                }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(modifier: Modifier, favorites: Set<String>, onFood: (Food) -> Unit, onFavorite: (Food) -> Unit, onAdd: (Food) -> Unit) {
    val items = foods.filter { it.id in favorites }
    LazyColumn(modifier.fillMaxSize().padding(18.dp)) {
        item {
            Text("Mis favoritos", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Tus platillos guardados para pedir después", color = Gray)
            Spacer(Modifier.height(18.dp))
        }
        if (items.isEmpty()) item { EmptyState("❤️", "Aún no tienes favoritos", "Toca el corazón de un platillo para guardarlo.") }
        else items(items) { food -> FoodCard(food, true, onAdd, onFood, onFavorite); Spacer(Modifier.height(12.dp)) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DetailScreen(modifier: Modifier, food: Food, favorite: Boolean, onBack: () -> Unit, onFavorite: () -> Unit, onAdd: (Int) -> Unit) {
    var quantity by remember(food.id) { mutableStateOf(1) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            Text("Detalle", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (favorite) Orange else Gray) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = SoftOrange), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) { Text(food.emoji, fontSize = 130.sp) }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(food.category.uppercase(), color = Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("★ ${food.rating}", color = OrangeDark, fontWeight = FontWeight.Bold)
            }
            Text(food.name, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(money(food.price), color = Orange, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Text(food.description, color = Gray, fontSize = 16.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(Icons.Default.Schedule, food.time)
                InfoPill(Icons.Default.LocalShipping, "Entrega")
            }
            Spacer(Modifier.height(18.dp))
            Text("Cantidad", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityButton(Icons.Default.Remove, quantity > 1) { quantity-- }
                Text(quantity.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 22.dp))
                QuantityButton(Icons.Default.Add, true) { quantity++ }
            }
            Spacer(Modifier.height(26.dp))
            Button(onClick = { onAdd(quantity) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
                Icon(Icons.Default.ShoppingCart, null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar • ${money(food.price * quantity)}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(25.dp))
        }
    }
}

@Composable
private fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Orange, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(text, fontSize = 12.sp)
        }
    }
}

@Composable
private fun QuantityButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    FilledIconButton(onClick = onClick, enabled = enabled, colors = IconButtonDefaults.filledIconButtonColors(containerColor = SoftOrange, contentColor = Orange)) { Icon(icon, null) }
}

@Composable
private fun CartScreen(modifier: Modifier, cart: List<CartItem>, onIncrease: (Food) -> Unit, onDecrease: (Food) -> Unit, onClear: () -> Unit, onCheckout: () -> Unit, onContinue: () -> Unit) {
    val subtotal = cart.sumOf { it.food.price * it.quantity }
    val delivery = if (cart.isEmpty()) 0.0 else if (subtotal >= 20) 0.0 else 1.50
    val total = subtotal + delivery
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mi carrito", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (cart.isNotEmpty()) TextButton(onClick = onClear) { Text("Vaciar") }
        }
        if (cart.isEmpty()) {
            EmptyState("🛒", "Tu carrito está vacío", "Agrega algo delicioso para comenzar.", onContinue)
        } else {
            if (subtotal >= 20) FreeShippingBanner()
            else Text("Agrega ${money(20 - subtotal)} para obtener envío gratis 🚚", color = Green, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(cart) { item -> CartItemCard(item, onIncrease, onDecrease) }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    SummaryRow("Subtotal", subtotal)
                    SummaryRow("Envío", delivery)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(money(total), fontSize = 21.sp, color = Orange, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("Continuar al pago", fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun FreeShippingBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = SoftGreen), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalShipping, null, tint = Green); Spacer(Modifier.width(9.dp)); Text("¡Tienes envío gratis!", color = Green, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CartItemCard(item: CartItem, onIncrease: (Food) -> Unit, onDecrease: (Food) -> Unit) {
    Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(SoftOrange), contentAlignment = Alignment.Center) { Text(item.food.emoji, fontSize = 31.sp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.food.name, fontWeight = FontWeight.Bold)
                Text(money(item.food.price * item.quantity), color = Orange, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDecrease(item.food) }) { Icon(Icons.Default.RemoveCircleOutline, "Disminuir") }
                Text(item.quantity.toString(), fontWeight = FontWeight.Bold)
                IconButton(onClick = { onIncrease(item.food) }) { Icon(Icons.Default.AddCircle, "Aumentar", tint = Orange) }
            }
        }
    }
}

@Composable
private fun CheckoutScreen(modifier: Modifier, cart: List<CartItem>, onBack: () -> Unit, onDone: (String, String, Double, Double, Double, Double) -> Unit) {
    var address by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Efectivo") }
    var coupon by remember { mutableStateOf("") }
    var couponApplied by remember { mutableStateOf(false) }
    val subtotal = cart.sumOf { it.food.price * it.quantity }
    val delivery = if (subtotal >= 20) 0.0 else 1.50
    val discount = if (couponApplied) subtotal * .20 else 0.0
    val total = max(0.0, subtotal + delivery - discount)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            Text("Finalizar pedido", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle("📍 Dirección de entrega")
        OutlinedTextField(address, { address = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("¿Dónde lo entregamos?") }, placeholder = { Text("Ej. Colonia Escalón, San Salvador") }, shape = RoundedCornerShape(15.dp))
        Spacer(Modifier.height(18.dp))
        SectionTitle("💳 Método de pago")
        listOf("Efectivo", "Tarjeta").forEach { method ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { payment = method }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(payment == method, { payment = method })
                    Icon(if (method == "Efectivo") Icons.Default.Payments else Icons.Default.CreditCard, null, tint = Orange)
                    Spacer(Modifier.width(8.dp)); Text(method, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("🎁 Cupón de descuento")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(coupon, { coupon = it.uppercase() }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Código") }, shape = RoundedCornerShape(14.dp), enabled = !couponApplied)
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (coupon == "BIENVENIDO") couponApplied = true }, enabled = !couponApplied && coupon.isNotBlank(), shape = RoundedCornerShape(13.dp)) { Text(if (couponApplied) "Aplicado" else "Aplicar") }
        }
        if (couponApplied) Text("✓ 20% de descuento aplicado", color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
        else Text("Prueba BIENVENIDO para obtener 20% de descuento.", color = Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        SectionTitle("🧾 Resumen")
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(19.dp)) {
            Column(Modifier.padding(16.dp)) {
                SummaryRow("Subtotal", subtotal)
                SummaryRow("Envío", delivery)
                if (discount > 0) SummaryRow("Descuento", -discount)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total a pagar", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(money(total), color = Orange, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onDone(address.trim(), payment, subtotal, delivery, discount, total) }, enabled = address.isNotBlank() && cart.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(16.dp)) { Text("Confirmar pedido", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun OrdersScreen(modifier: Modifier, orders: List<Order>) {
    if (orders.isEmpty()) {
        Column(modifier.fillMaxSize().padding(18.dp)) {
            Text("Mis pedidos", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            EmptyState("📦", "Todavía no tienes pedidos", "Cuando realices uno aparecerá aquí.")
        }
        return
    }
    LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mis pedidos", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold); Text("Historial de tus pedidos", color = Gray) }
        items(orders) { order -> OrderCard(order) }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun OrderCard(order: Order) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(SoftOrange), contentAlignment = Alignment.Center) { Text("🛍️", fontSize = 25.sp) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) { Text("Pedido #${order.number}", fontWeight = FontWeight.Bold); Text(order.date, color = Gray, fontSize = 12.sp) }
                Text(money(order.total), color = Orange, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(13.dp))
            StatusTimeline()
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                order.items.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.emoji, fontSize = 22.sp); Spacer(Modifier.width(8.dp)); Text("${item.quantity} × ${item.name}", Modifier.weight(1f)); Text(money(item.price * item.quantity), fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("📍 ${order.address}", color = Gray, fontSize = 12.sp)
                Text("💳 ${order.payment}", color = Gray, fontSize = 12.sp)
            }
            Text(if (expanded) "Ocultar detalles" else "Ver detalles", color = Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun StatusTimeline() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(true); TimelineLine(); StatusDot(false); TimelineLine(); StatusDot(false)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recibido", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Preparando", color = Gray, fontSize = 11.sp)
            Text("En camino", color = Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) { Box(Modifier.size(13.dp).clip(CircleShape).background(if (active) Green else Color.LightGray)) }
@Composable
private fun TimelineLine() { Box(Modifier.width(75.dp).height(3.dp).background(Color.LightGray)) }

@Composable
private fun SectionTitle(text: String) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }

@Composable
private fun SummaryRow(label: String, value: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Gray); Text(money(value), fontWeight = FontWeight.Medium) }
}

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String, action: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().padding(vertical = 70.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 62.sp)
            Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(subtitle, color = Gray)
            if (action != null) { Spacer(Modifier.height(14.dp)); OutlinedButton(action, shape = RoundedCornerShape(13.dp)) { Text("Explorar comida") } }
        }
    }
}
