package br.com.casadocodigo.loja.service;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import br.com.casadocodigo.loja.beans.PagamentoGateway;
import br.com.casadocodigo.loja.daos.CompraDao;
import br.com.casadocodigo.loja.infra.MailSender;
import br.com.casadocodigo.loja.model.Compra;
@Path("/pagamento")
public class PagamentoService {

    @Context
    ServletContext context;

    @Inject
    private CompraDao compraDao;

    @Inject
    private PagamentoGateway pagamentoGateway;
    
    @Inject
    private MailSender mailSender;
    
    @Inject
    private JMSContext jmsContext;

    private static ExecutorService executor = Executors.newFixedThreadPool(50);
    
    @Resource(name="java:/jms/topics/CarrinhoComprasTopico")
    private Destination destination;
    
    @GET
    public String teste() {
    	return "teste";
    }

    @POST
    public void pagar(@Suspended final AsyncResponse ar, @QueryParam("uuid") String uuid) {
        Compra compra = compraDao.buscaPorUuid(uuid);

        String contextPath = context.getContextPath();
        
        JMSProducer producer = jmsContext.createProducer();


        executor.submit(() -> {
            try {
                String resposta = pagamentoGateway.pagar(compra.getTotal());
                
                producer.send(destination, compra.getUuid());

                URI responseUri = UriBuilder
                        .fromPath("http://localhost:8080" + contextPath + "/index.xhtml")
                        .queryParam("msg", "Compra realizada com sucesso").build();

                Response response = Response.seeOther(responseUri).build();
                
                String messageBody = "Sua compra foi realizada com sucesso, com o número de pedido " + compra.getUuid();

                
                
                ar.resume(response);
            } catch (Exception e) {
                ar.resume(new WebApplicationException(e));
            }
        });
    }
}