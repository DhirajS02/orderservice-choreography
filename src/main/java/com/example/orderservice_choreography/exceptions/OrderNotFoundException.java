package com.example.orderservice_choreography.exceptions;

public class OrderNotFoundException extends RuntimeException{
    public OrderNotFoundException(String msg)
    {
        super(msg);
    }
}
