package eu.h2020.symbiote.pr.model;

import org.springframework.data.annotation.Id;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
public class PersistentVariable {

    @Id
    private String variableName;
    private Object value;

    public PersistentVariable(String variableName, Object value) {
        this.variableName = variableName;
        this.value = value;
    }

    public String getVariableName() { return variableName; }
    public void setVariableName(String variableName) { this.variableName = variableName; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
