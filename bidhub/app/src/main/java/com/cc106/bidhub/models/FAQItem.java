package com.cc106.bidhub.models;

/**
 * Model class for FAQ items
 */
public class FAQItem {
    private String question;
    private String answer;
    private boolean isExpanded;
    
    public FAQItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.isExpanded = false;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public boolean isExpanded() {
        return isExpanded;
    }
    
    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
    
    public void toggleExpanded() {
        isExpanded = !isExpanded;
    }
}
