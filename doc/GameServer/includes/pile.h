/*
 * pile.h
 *
 *  Created on: Oct 12, 2013
 *      Author: nth
 */

#ifndef PILE_H_
#define PILE_H_

#include <stdio.h>
#include <stdlib.h>

#include "utilities.h"

typedef struct pile
{
	struct pile *next;
	int index;
}*Pile;

Pile putOnTopOfPile(Pile p, int index);
Pile removeTopOfPile(Pile p);
int topOfPile(Pile p);
int isEmptyPile(Pile p);

Pile generatePileOfIndex(int length);

#endif /* PILE_H_ */
