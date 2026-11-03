package com.workshoputilities.client;


/** Dipasang ke HandledScreen lewat mixin: menyimpan {@link PanelController} untuk GUI vanilla yang diberi panel (null bila tidak). */
public interface PanelScreenAccess {
	PanelController workshop_utilities$getPanelController();
}
