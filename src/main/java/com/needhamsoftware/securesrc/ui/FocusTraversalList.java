package com.needhamsoftware.securesrc.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.List;

class FocusTraversalList extends FocusTraversalPolicy {

  private final List<Component> focusItems;

  public FocusTraversalList(List<Component> focusItems) {
    this.focusItems = focusItems;
  }

  @Override
  public Component getComponentAfter(Container aContainer, Component aComponent) {
    if (focusItems.contains(aComponent)) {
      if (getLastComponent(aContainer).equals(aComponent)) {
        return getFirstComponent(aContainer);
      } else {
        return focusItems.get(focusItems.indexOf(aComponent) + 1);
      }
    } else {
      return getDefaultComponent(aContainer);
    }
  }

  @Override
  public Component getComponentBefore(Container aContainer, Component aComponent) {
    if (focusItems.contains(aComponent)) {
      if (getFirstComponent(aContainer).equals(aComponent)) {
        return getLastComponent(aContainer);
      } else {
        return focusItems.get(focusItems.indexOf(aComponent) - 1);
      }
    } else {
      return getDefaultComponent(aContainer);
    }
  }

  @Override
  public Component getFirstComponent(Container aContainer) {
    return focusItems.getFirst();
  }

  @Override
  public Component getLastComponent(Container aContainer) {
    return focusItems.getLast();
  }

  @Override
  public Component getDefaultComponent(Container aContainer) {
    return focusItems.getFirst();
  }
}
