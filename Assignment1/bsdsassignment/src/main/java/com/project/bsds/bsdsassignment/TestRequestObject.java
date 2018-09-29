/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.project.bsds.bsdsassignment;

/**
 *
 * @author manika2211
 */

public class TestRequestObject {
    private String input;
    public TestRequestObject() {
    }
    public TestRequestObject(String input) {
        this.input = input;
    }
    public String getInput() {
        return input;
    }
    public void setInput(String input) {
        this.input = input;
    }
    
    @Override
    public String toString() {
       return "input is " + input;
    }
}
