package com.github.mjjaniec.lmq.components;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;

@Tag("audio")
public class Audio extends HtmlContainer {

    private static final PropertyDescriptor<String, String> srcDescriptor = PropertyDescriptors.attributeWithDefault("src", "");

    public Audio(String src) {
        set(srcDescriptor, src);
    }

    public void play() {
        getElement().executeJs("this.play()");
    }

}
