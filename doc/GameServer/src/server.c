/*
 * server.c
 *
 *  Created on: Nov 2, 2013
 *      Author: nth
 */

#include "../includes/server.h"

int main(int argc, char *argv[])
{
	traceMessage("Server will be started ...");

	// on retrouve de façon dynamique le repertoire parent du programme.
	int i = strlen(argv[0]);

	while(argv[0][i] != '/')
		i--;

	if(i == 0)
		i = 1;

	argv[0][i] = '\0';

	// on repositionne le repertoire de travail.
	// Indispensable pour accéder aux fichiers de configuration de l'application si celle-ci est lance depuis un autre repertoire que celui qui contient l'application.
	chdir(argv[0]);

	authentificationThread();
}

