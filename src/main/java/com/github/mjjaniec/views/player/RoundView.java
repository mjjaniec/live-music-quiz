package com.github.mjjaniec.views.player;

import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

@Route(value = "round2", layout = PlayerView.class)
public class RoundView extends VerticalLayout implements HasUrlParameter<String> {

    private final VerticalLayout progress = new VerticalLayout();

    public RoundView() {
        setSpacing(false);
        setPadding(false);
        progress.getStyle().setBackground(Palete.BLUE);
        progress.setPadding(false);
        progress.setSpacing(false);


        add(progress);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        String[] elems = parameter.split("-");
        progress.add(createProgress("R:", Integer.parseInt(elems[0]), Integer.parseInt(elems[1]), Palete.DARKER));
        progress.add(createProgress("S:", Integer.parseInt(elems[2]), Integer.parseInt(elems[3]), Palete.LIGHTER));

    }

    private HorizontalLayout createProgress(String label, int step, int of, String color) {
        HorizontalLayout result = new HorizontalLayout();
        result.getStyle().setColor(Palete.WHITE);
        result.setSpacing(false);
        result.setPadding(false);
        result.setWidthFull();
        VerticalLayout left = new VerticalLayout();
        left.setPadding(false);
        left.getStyle().setPaddingLeft("1em").setPaddingTop("0.1em");
        int leftW = step * 100 / of;
        left.setWidth(leftW + "%");
        left.setHeight("2em");
        left.add(new Text(label + " " + step + " / " + of));
        Div right = new Div();
        left.getStyle().setBackgroundColor(color);
        right.setWidth((100 - leftW) + "%");


        result.add(left, right);

        return result;
    }
}
