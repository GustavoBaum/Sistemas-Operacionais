#include <stdio.h>
#include <pthread.h>

int contador_global = 0;
pthread_mutex_t lock;

void* rotina_thread(void* arg) {
    for (int i = 0; i < 100000; i++) {
        pthread_mutex_lock(&lock);   // Tranca o cadeado
        contador_global++;           // Região Crítica protegida
        pthread_mutex_unlock(&lock); // Destranca o cadeado
    }
    return NULL;
}

int main() {
    pthread_t t1, t2;
    
    pthread_mutex_init(&lock, NULL);

    pthread_create(&t1, NULL, rotina_thread, NULL);
    pthread_create(&t2, NULL, rotina_thread, NULL);

    pthread_join(t1, NULL);
    pthread_join(t2, NULL);

    pthread_mutex_destroy(&lock);
    printf("Valor final do contador: %d (Esperado: 200000)\n", contador_global);
    
    return 0;
}