
all : ./obj ./obj/pile.o ./obj/network.o ./obj/accounts_management.o ./obj/game_management.o ./obj/utilities.o ./bin/server

./obj/pile.o : ./includes/pile.h ./src/pile.c
	gcc -g -c ./src/pile.c -o ./obj/pile.o

./obj/network.o : ./src/network.c ./includes/network.h ./includes/utilities.h 
	gcc -g -c ./src/network.c -o ./obj/network.o

./obj/accounts_management.o : ./src/accounts_management.c ./includes/accounts_management.h ./includes/network.h ./includes/utilities.h
	gcc -g -c ./src/accounts_management.c -o ./obj/accounts_management.o

./obj/game_management.o : ./src/game_management.c ./includes/game_management.h ./includes/network.h ./includes/utilities.h
	gcc -g -c ./src/game_management.c -o ./obj/game_management.o

./obj/utilities.o : ./src/utilities.c ./includes/utilities.h
	gcc -g -c ./src/utilities.c -o ./obj/utilities.o

./obj/server.o : ./src/server.c ./includes/server.h ./includes/utilities.h ./includes/accounts_management.h ./includes/pile.h ./includes/network.h ./includes/game_management.h
	gcc -g -c ./src/server.c -o ./obj/server.o

./bin/server : ./obj/server.o ./obj/network.o ./obj/accounts_management.o ./obj/game_management.o ./obj/utilities.o ./obj/pile.o ./bin
	gcc -g -pthread ./obj/server.o ./obj/network.o ./obj/accounts_management.o ./obj/game_management.o ./obj/utilities.o ./obj/pile.o -o ./bin/server

./bin :
	mkdir ./bin

./obj :
	mkdir ./obj

clean : 
	rm ./obj/*.o
