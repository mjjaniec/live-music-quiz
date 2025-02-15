package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;


@Route(value = "round-init", layout = BigScreenView.class)
public class RoundInitView extends VerticalLayout implements BigScreenRoute {

    private final VerticalLayout progress = new VerticalLayout();

    public RoundInitView(GameService gameService) {
        setSpacing(false);
        setPadding(false);
        progress.getStyle().setBackground(Palete.BLUE);
        progress.setPadding(false);
        progress.setSpacing(false);

        gameService.stage().asRoundInit()
                .ifPresent(roundInit ->
                        progress.add(createProgress("R:", roundInit.roundNumber().number(), roundInit.roundNumber().of(), Palete.DARKER)));

        add(progress);
    }


    private HorizontalLayout createProgress(String label, long step, long of, String color) {
        HorizontalLayout result = new HorizontalLayout();
        result.getStyle().setColor(Palete.WHITE);
        result.setSpacing(false);
        result.setPadding(false);
        result.setWidthFull();
        VerticalLayout left = new VerticalLayout();
        left.setPadding(false);
        left.getStyle().setPaddingLeft("1em").setPaddingTop("0.1em");
        long leftW = step * 100 / of;
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
