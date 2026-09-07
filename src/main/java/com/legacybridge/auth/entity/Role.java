package com.legacybridge.auth.entity;

/**
 * El nombre de cada constante ya es el "granted authority" completo que
 * usara Spring Security (ROLE_USER, ROLE_ADMIN). Se evita asi la
 * concatenacion implicita de prefijos que hace hasRole(...) y se deja el
 * mapeo explicito: se usa hasAuthority('ROLE_ADMIN') en vez de
 * hasRole('ADMIN').
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
