#include <stdio.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>

sem_t mesas_restaurante;

void* cliente(void* arg) {
    int id_cliente = *(int*)arg;
    
    printf("Cliente %d: Chegou na porta e quer entrar.\n", id_cliente);
    
    // Tenta pegar uma mesa. Se o semáforo for 0, o cliente dorme aqui na fila.
    sem_wait(&mesas_restaurante); 
    
    printf("  -> Cliente %d: [ENTROU] Conseguiu uma mesa e esta comendo!\n", id_cliente);
    sleep(3); // Simula o tempo comendo
    
    printf("  <- Cliente %d: [SAIU] Terminou de comer e liberou a mesa.\n", id_cliente);
    
    // Aumenta o contador do semáforo, acordando o próximo da fila.
    sem_post(&mesas_restaurante);
    
    return NULL;
}

int main() {
    pthread_t clientes[6];
    int ids[6] = {1, 2, 3, 4, 5, 6};
    
    // Inicia o semáforo com 3 vagas (capacidade máxima do restaurante)
    sem_init(&mesas_restaurante, 0, 3);
    
    // 5 clientes chegam de uma vez
    for (int i = 0; i < 6; i++) {
        pthread_create(&clientes[i], NULL, cliente, &ids[i]);
    }
    
    for (int i = 0; i < 6; i++) {
        pthread_join(clientes[i], NULL);
    }
    
    sem_destroy(&mesas_restaurante);
    return 0;
}