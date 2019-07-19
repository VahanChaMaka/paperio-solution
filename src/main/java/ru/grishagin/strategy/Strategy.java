package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;

public interface Strategy {

    Direction onTick();
}
