package com.pagueibaratoapi.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pagueibaratoapi.models.requests.Produto;
import com.pagueibaratoapi.models.responses.ResponsePagina;
import com.pagueibaratoapi.models.responses.ResponseProduto;
import com.pagueibaratoapi.repository.ProdutoRepository;
import com.pagueibaratoapi.utils.PaginaUtils;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/produto")
public class ProdutoController {
    
    private final ProdutoRepository produtoRepository;

    public ProdutoController(ProdutoRepository produtoRepository){
        this.produtoRepository = produtoRepository;
    }

    @PostMapping
    public ResponseProduto criar(@RequestBody Produto requestProduto){
        // Criando uma nova instância do produto para tratar o nome dele e criá-lo no banco
        Produto produtoTratado = requestProduto;

        // Pegando cada palavra do nome do produto separado por espaço e em minúsculas
        String[] nomeProduto = requestProduto.getNome().toLowerCase().split(" ");

        // Percorrendo cada palavra do nome do produto
        for(int i = 0; i < nomeProduto.length; i++){
            // Transformando a palavra atual em uma array
            char[] palavraArr = nomeProduto[i].toCharArray();

            // Transformando a primeira letra da palavra em maiúscula
            palavraArr[0] = nomeProduto[i].toUpperCase().charAt(0);

            // Reescreve a palavra atual com a primeira letra tratada
            nomeProduto[i] = String.valueOf(palavraArr);

            // Se for a primeira palavra sendo tratada, substitui o nome do produto pelo nome tratado
            if(i < 1)
                produtoTratado.setNome(nomeProduto[i]);
            // Se não, concatena a palavra atual ao nome do produto
            else
                produtoTratado.setNome(produtoTratado.getNome() + " " + nomeProduto[i]);
        }

        ResponseProduto responseProduto = new ResponseProduto(produtoRepository.save(produtoTratado));

        responseProduto.add(
            linkTo(
                methodOn(ProdutoController.class).ler(responseProduto.getId())
            )
            .withSelfRel()
        );

        return responseProduto;
    }

    @GetMapping("/{id}")
    public ResponseProduto ler(@PathVariable(value = "id") Integer id){
        ResponseProduto responseProduto = new ResponseProduto(produtoRepository.findById(id).get());
        
        if(responseProduto != null){
            responseProduto.add(
                linkTo(
                    methodOn(ProdutoController.class).listar(new Produto())
                )
                .withRel("collection")
            );
        }
        
        return responseProduto;
    }

    @GetMapping
    public List<ResponseProduto> listar(Produto requestProduto){

        List<Produto> produtos = produtoRepository.findAll(
            Example.of(requestProduto, ExampleMatcher
                                .matching()
                                .withIgnoreCase()
                                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)));

        List<ResponseProduto> responseProduto = new ArrayList<ResponseProduto>();

        for(Produto produto : produtos){
            responseProduto.add(new ResponseProduto(produto));
        }

        if(responseProduto != null){
            for(ResponseProduto produto : responseProduto){
                produto.add(
                    linkTo(
                        methodOn(ProdutoController.class).ler(produto.getId())
                    )
                    .withSelfRel()
                );
            }
        }

        return responseProduto;
    }

    @GetMapping(params = {"pagina", "limite"})
    public ResponsePagina listar(Produto requestProduto, @RequestParam(required = false, defaultValue = "0") Integer pagina, @RequestParam(required = false, defaultValue = "10") Integer limite){

        Page<Produto> paginaProduto = produtoRepository.findAll(
            Example.of(requestProduto, ExampleMatcher
                                .matching()
                                .withIgnoreCase()
                                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)), 
            PageRequest.of(pagina, limite));

        List<ResponseProduto> produtos = new ArrayList<ResponseProduto>();

        ResponsePagina responseProduto = PaginaUtils.criarResposta(pagina, limite, paginaProduto, produtos);

        for(Produto produto : paginaProduto.getContent()){
            produtos.add(new ResponseProduto(produto));
        }

        responseProduto.add(
            linkTo(
                methodOn(ProdutoController.class).listar(requestProduto, 0, limite)
            )
            .withRel("first")
        );

        if(!paginaProduto.isEmpty()){
            if(pagina > 0){
                responseProduto.add(
                    linkTo(
                        methodOn(ProdutoController.class).listar(requestProduto, pagina-1, limite)
                    )
                    .withRel("previous")
                );
            }
            if(pagina < paginaProduto.getTotalPages()-1){
                responseProduto.add(
                    linkTo(
                        methodOn(ProdutoController.class).listar(requestProduto, pagina+1, limite)
                    )
                    .withRel("next")
                );
            }
            responseProduto.add(
                linkTo(
                    methodOn(ProdutoController.class).listar(requestProduto, paginaProduto.getTotalPages()-1, limite)
                )
                .withRel("last")
            );

            for(ResponseProduto produto : produtos){
                produto.add(
                    linkTo(
                        methodOn(ProdutoController.class).ler(produto.getId())
                    )
                    .withSelfRel()
                );
            }
        }

        return responseProduto;
    }

    @PatchMapping("/{id}")
    public ResponseProduto editar(@PathVariable int id, @RequestBody Produto requestProduto){
        Produto produtoAtual = produtoRepository.findById(id).get();

        if(requestProduto.getNome() != null)
            produtoAtual.setNome(requestProduto.getNome());

        if(requestProduto.getMarca() != null)
            produtoAtual.setMarca(requestProduto.getMarca());
        
        if(requestProduto.getTamanho() != null)
            produtoAtual.setTamanho(requestProduto.getTamanho());

        if(requestProduto.getCor() != null){
            if(requestProduto.getCor() == "")
                produtoAtual.setCor(null);
            else
                produtoAtual.setCor(requestProduto.getCor());
        }

        if(requestProduto.getCategoriaId() != null)
            produtoAtual.setCategoriaId(requestProduto.getCategoriaId());

        ResponseProduto responseProduto = new ResponseProduto(produtoRepository.save(produtoAtual));

        responseProduto.add(
            linkTo(
                methodOn(ProdutoController.class).ler(responseProduto.getId())
            )
            .withSelfRel()
        );

        return responseProduto;
    }

    @PutMapping("/{id}")
    public ResponseProduto atualizar(@PathVariable int id, @RequestBody Produto requestProduto){
        requestProduto.setId(id);

        ResponseProduto responseProduto = new ResponseProduto(produtoRepository.save(requestProduto));

        responseProduto.add(
            linkTo(
                methodOn(ProdutoController.class).ler(responseProduto.getId())
            )
            .withSelfRel()
        );

        return responseProduto;
    }

    @DeleteMapping("/{id}")
    public Object deletar(@PathVariable int id){
        produtoRepository.deleteById(id);

        return linkTo(
                    methodOn(ProdutoController.class).listar(new Produto())
                )
                .withRel("collection");
    }

}
