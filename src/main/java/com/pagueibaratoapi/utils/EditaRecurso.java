package com.pagueibaratoapi.utils;

import com.pagueibaratoapi.models.Usuario;

public class EditaRecurso {
    
    public static Usuario editarUsuario(Usuario usuarioAtual, Usuario usuarioEditado){
        return PatchUsuario.edita(usuarioAtual, usuarioEditado);
    }
    
}