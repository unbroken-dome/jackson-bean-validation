package org.unbrokendome.jackson.beanvalidation.path;

public class KeyedPropertyNode extends AbstractPropertyNode {

    private final Object key;

    public KeyedPropertyNode(String name, Object key) {
        super(name);
        this.key = key;
    }

    @Override
    public Object getKey() {
        return key;
    }
}
