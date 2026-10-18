#include <stdio.h>
#include <pthread.h>
#include <unistd.h>

int saldo = 1000;
pthread_mutex_t cofre_mutex;

void* operacao_bancaria(void* arg) {
    int id_caixa = *(int*)arg;
    
    printf("[Caixa %d] Aguardando liberacao do sistema...\n", id_caixa);
    
    // A thread tenta pegar a chave. Se outra thread já tiver, ela pausa aqui.
    pthread_mutex_lock(&cofre_mutex);  
    
    printf("\n[Caixa %d] >>> MUTEX BLOQUEADO (Entrou no cofre) <<<\n", id_caixa);
    printf("[Caixa %d] Lendo saldo atual: R$ %d\n", id_caixa, saldo);
    sleep(2); // Simula o tempo da transação
    
    saldo -= 100; // Saca R$ 100
    printf("[Caixa %d] Saque realizado. Novo saldo: R$ %d\n", id_caixa, saldo);
    
    printf("[Caixa %d] <<< MUTEX LIBERADO (Saiu do cofre) <<<\n", id_caixa);
    
    // Devolve a chave, permitindo que o outro caixa entre.
    pthread_mutex_unlock(&cofre_mutex); 
    
    return NULL;
}

int main() {
    pthread_t caixa1, caixa2;
    int id1 = 1, id2 = 2;
    
    pthread_mutex_init(&cofre_mutex, NULL);
    
    // Inicia os dois caixas quase ao mesmo tempo
    pthread_create(&caixa1, NULL, operacao_bancaria, &id1);
    pthread_create(&caixa2, NULL, operacao_bancaria, &id2);
    
    pthread_join(caixa1, NULL);
    pthread_join(caixa2, NULL);
    
    pthread_mutex_destroy(&cofre_mutex);
    return 0;
}