/*
 * piles.c
 *
 *  Created on: Oct 12, 2013
 *      Author: nth
 */

#include "../includes/pile.h"

int isEmptyPile(Pile p)
{
	return p == NULL;
}

Pile putOnTopOfPile(Pile p, int index)
{
	Pile tmp = (Pile) malloc(sizeof(struct pile));
	tmp->index = index;
	tmp->next = p;
	return tmp;
}

Pile removeTopOfPile(Pile p)
{
	if (p == NULL)
		return NULL;

	Pile tmp = p->next;
	free(p);
	return tmp;
}

int topOfPile(Pile p)
{
	return (p == NULL ? -1 : p->index);
}

Pile generatePileOfIndex(int length)
{
	Pile p = NULL;
	int i;
	for (i = 0; i < length; i++)
		p = putOnTopOfPile(p, i);

	return p;
}
