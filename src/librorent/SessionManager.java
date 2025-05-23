package librorent;

public class SessionManager {
    private static SessionManager instance;
    private int currentUserId;
    private boolean isLoggedIn;
    
    private SessionManager() {
        this.currentUserId = 0;
        this.isLoggedIn = false;
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void login(int userId) {
        this.currentUserId = userId;
        this.isLoggedIn = true;
    }
    
    public void logout() {
        this.currentUserId = 0;
        this.isLoggedIn = false;
    }
    
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    
    public int getCurrentUserId() {
        return currentUserId;
    }
} 