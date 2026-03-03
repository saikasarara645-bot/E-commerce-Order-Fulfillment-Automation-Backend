
import java.io.BufferedReader;
import java.security.MessageDigest;

/** Admin.java – Admin model and authentication logic (password hashing for secure login) */
public class Admin {
    public static final String ANSI_Yellow ="\u001B[38;5;220m";
    // ===== Pastel Theme (Girlish + Professional) =====
    public static final String RESET = "\u001B[0m";
    public static final String BOLD  = "\u001B[1m";

    // Soft pastel colors
    public static final String PINK      = "\u001B[38;5;211m"; // header / highlight
    public static final String MINT      = "\u001B[38;5;156m"; // success/allowed
    //public static final String PEACH     = "\u001B[38;5;216m"; // warnings/restricted
    public static final String ROSE      = "\u001B[38;5;174m"; // exit/error
     public static final String SOFTGRAY  = "\u001B[38;5;250m"; // normal text