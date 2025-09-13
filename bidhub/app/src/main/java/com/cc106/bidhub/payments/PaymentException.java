package com.cc106.bidhub.payments;

/**
 * Payment Exception
 * Custom exception for payment processing errors
 */
public class PaymentException extends Exception {
    private PaymentError paymentError;
    
    public PaymentException(PaymentError paymentError) {
        super(paymentError.getErrorMessage());
        this.paymentError = paymentError;
    }
    
    public PaymentException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.paymentError = new PaymentError(errorCode, errorMessage);
    }
    
    public PaymentException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.paymentError = new PaymentError(errorCode, errorMessage);
        if (cause != null) {
            this.paymentError.setStackTrace(getStackTraceString(cause));
        }
    }
    
    public PaymentError getPaymentError() {
        return paymentError;
    }
    
    private String getStackTraceString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    @Override
    public String toString() {
        return "PaymentException{" +
                "paymentError=" + paymentError +
                '}';
    }
}
