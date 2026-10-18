#include <stdio.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>

sem_t semaforo_sinal;

void* produtor(void* arg) {
    printf("[Produtor] Trabalhando e gerando dados...\n");
    sleep(1); // Simula processamento
    printf("[Produtor] Dados prontos! Sinalizando o consumidor.\n");
    
    // Incrementa o semáforo (libera a thread bloqueada)
    sem_post(&semaforo_sinal); 
    return NULL;
}

void* consumidor(void* arg) {
    printf("[Consumidor] Aguardando os dados do produtor...\n");
    
    // Decrementa o semáforo. Se for 0, bloqueia a thread até o produtor sinalizar.
    sem_wait(&semaforo_sinal); 
    
    printf("[Consumidor] Sinal recebido! Processando os dados.\n");
    return NULL;
}

int main() {
    pthread_t t_produtor, t_consumidor;
    
    // Inicializa o semáforo com o valor 0 (consumidor começará bloqueado)
    sem_init(&semaforo_sinal, 0, 0);

    pthread_create(&t_consumidor, NULL, consumidor, NULL);
    pthread_create(&t_produtor, NULL, produtor, NULL);

    pthread_join(t_consumidor, NULL);
    pthread_join(t_produtor, NULL);

    sem_destroy(&semaforo_sinal);
    return 0;
}