package org.unbrokendome.jackson.beanvalidation.path;

public class IndexedPropertyNode extends AbstractPropertyNode {

    private final int index;

    public IndexedPropertyNode(String name, int index) {
        super(name);
        this.index = index;
    }

    @Override
    public Integer getIndex() {
        return index;
    }
}
