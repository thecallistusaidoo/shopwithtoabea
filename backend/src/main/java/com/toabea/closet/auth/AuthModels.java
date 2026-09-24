package com.toabea.closet.auth;
public final class AuthModels { private AuthModels(){} public record LoginRequest(String username,String password){} public record LoginResponse(String token,String username){} }
