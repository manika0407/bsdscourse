/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.project.bsds.bsdsassignment;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
/**
 *
 * @author manika2211
 */

public class LambdaTestHandler implements RequestHandler<TestRequestObject, String> {
    @Override
    public String handleRequest(TestRequestObject input, Context context) {
       return "alive -> input: " + input.getInput();
    }
}
