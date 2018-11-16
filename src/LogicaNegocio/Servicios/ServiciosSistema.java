package LogicaNegocio.Servicios;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import Controlador.Controlador;
import Controlador.TC;
import LogicaNegocio.Transfers.TransferAtributo;
import LogicaNegocio.Transfers.TransferConexion;
import LogicaNegocio.Transfers.TransferDominio;
import LogicaNegocio.Transfers.TransferEntidad;
import LogicaNegocio.Transfers.TransferRelacion;
import Persistencia.DAOAtributos;
import Persistencia.DAODominios;
import Persistencia.DAOEntidades;
import Persistencia.DAORelaciones;
import Persistencia.EntidadYAridad;
import Presentacion.Lenguajes.Lenguaje;
import Utilidades.HTMLUtils;
import Utilidades.ImagePath;
import Utilidades.TipoDominio;
import Utilidades.ConectorDBMS.ConectorDBMS;
import Utilidades.ConectorDBMS.FactoriaConectores;

public class ServiciosSistema {
	private Controlador controlador; 

	//atributos para la generacion de los modelos
	private boolean modeloValidado;
	private String sql="";
	private String sqlHTML="";
	private TransferConexion conexionScriptGenerado = null;

	//aqui se almacenaran las tablas ya creadas, organizadas por el id de la entidad /relacion (clave) y con el objeto tabla como valor.
	private Hashtable<Integer,Tabla> tablasEntidades=new Hashtable<Integer,Tabla>();
	private Hashtable<Integer,Tabla> tablasRelaciones=new Hashtable<Integer,Tabla>();
	private Vector<Tabla> tablasMultivalorados=new Vector<Tabla>();
	private Hashtable<Integer,Enumerado> tiposEnumerados = new Hashtable<Integer,Enumerado>();

	//validacion de atributos
	private boolean validaAtributos(){
		/* hemos de validar lo siguiente:
		 * - Cada atributo pertenece a una sola entidad
		 * - Cada atributo tiene un dominio definido
		 * - Los atributos multivalorados no son clave
		 * - 
		 */
		DAOAtributos daoAtributos= new DAOAtributos(this.controlador.getPath());
		Vector <TransferAtributo> atributos =daoAtributos.ListaDeAtributos();
		boolean valido=true;
		int i=0;
		TransferAtributo t = new TransferAtributo();
		while (i<atributos.size()&& valido){
			t=atributos.elementAt(i);													
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(Lenguaje.getMensaje(Lenguaje.RATIFYING))+HTMLUtils.toItalic(t.getNombre())+HTMLUtils.newLine());
			valido=validaFidelidadAtributo(t)&& validaDominioDeAtributo(t);
			if (t.getCompuesto()) valido=valido && validaCompuesto(t); 
			i++;
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.newLine()+HTMLUtils.newLine());
		}

		return valido;
	}

	//metodos privados de validacion de atributos.
	private boolean validaDominioDeAtributo(TransferAtributo ta){
		//comprueba que tenga dominio

		boolean valido=true;
		String dom = ta.getDominio();
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_ATTRIB_DOMAIN))+HTMLUtils.newLine());
		if (ta.getCompuesto()){
			if(!dom.equals("null")){
				valido=false;
				controlador.mensajeDesde_SS(TC.SS_Validacion, HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
						Lenguaje.getMensaje(Lenguaje.COMPOSED_ATTRIBUTE)+HTMLUtils.newLine());				
			}
			else controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
					Lenguaje.getMensaje(Lenguaje.CORRECT_DOMAIN)+HTMLUtils.newLine()); 
		}
		else{
			if (dom.equals("")||dom.equals("null")){ 
				valido =false;
				controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
						Lenguaje.getMensaje(Lenguaje.NO_DOMAIN)+HTMLUtils.newLine());
			}
			else{
				if (dom.contains("(")) dom=quitaParenDominio(dom);
				int i=0;
				while(i<TipoDominio.values().length && !TipoDominio.values()[i].equals(dom))i++;
				if (i>TipoDominio.values().length){
					controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
							Lenguaje.getMensaje(Lenguaje.UNKNOWN_DOMAIN)+HTMLUtils.newLine());
					valido=false;
				}
				else controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
						Lenguaje.getMensaje(Lenguaje.CORRECT_DOMAIN)+HTMLUtils.newLine());
			}
		}

		return  valido;
	}
	private boolean validaFidelidadAtributo(TransferAtributo ta){
		// comprueba si el atributo pertenece solo a una entidad.
		DAOAtributos daoAtributos= new DAOAtributos(this.controlador.getPath());
		DAOEntidades daoEntidades= new DAOEntidades(this.controlador.getPath());
		DAORelaciones daoRelaciones= new DAORelaciones(this.controlador.getPath());
		Vector <TransferAtributo> atributos =daoAtributos.ListaDeAtributos();
		Vector <TransferEntidad> entidades =daoEntidades.ListaDeEntidades();
		Vector <TransferRelacion> relaciones =daoRelaciones.ListaDeRelaciones();
		boolean valido=true;
		boolean enEntidad=false;
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_ATTRIBUTE))+HTMLUtils.newLine());
		TransferEntidad te= new TransferEntidad();
		TransferRelacion tr= new TransferRelacion();
		int cont =0;
		int i=0;
		while (i<entidades.size()&& cont<=1){
			te= entidades.elementAt(i);
			if (estaEnVectorDeEnteros(te.getListaAtributos(),ta.getIdAtributo())){
				cont++;
				enEntidad=true;
			}
			i++;
		}
		if (!enEntidad){
			cont=0;
			i=0;
			while (i<relaciones.size()&& cont<=1){
				tr= relaciones.elementAt(i);
				if (estaEnVectorDeEnteros(tr.getListaAtributos(),ta.getIdAtributo())){
					cont++;
				}
				i++;
			}
		}

		switch (cont){
		case 0:
			//entonces es un subatributo, comprobamos q no esta repetido entre los subatributos
			i=0;
			int contSubAtrib=0;
			TransferAtributo aux= new TransferAtributo();
			while (i<atributos.size()&& contSubAtrib<=1){
				aux= atributos.elementAt(i);
				if(aux.getCompuesto())
					if (estaEnVectorDeEnteros(aux.getListaComponentes(),ta.getIdAtributo())){
						contSubAtrib++;
					}
				i++;
			}
			if (contSubAtrib==1)controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
					Lenguaje.getMensaje(Lenguaje.ATTRIBUTE)+" "+ta.getNombre()+ Lenguaje.getMensaje(Lenguaje.ONE_ATTRIB_SUBATTRIB)+HTMLUtils.newLine());
			else	{
				controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.ATTRIBUTE)+" "+ta.getNombre()+Lenguaje.getMensaje(Lenguaje.MANY_ATTRIB_SUBATTRIB)+HTMLUtils.newLine());
				valido=false;
			}
			break;
		case 1:
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
					Lenguaje.getMensaje(Lenguaje.ATTRIBUTE)+" "+ta.getNombre()+Lenguaje.getMensaje(Lenguaje.ONE_ENTITY)+HTMLUtils.newLine());
			break;
		default:
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.ATTRIBUTE)+" "+ta.getNombre()+Lenguaje.getMensaje(Lenguaje.MANY_ENTITIES)+HTMLUtils.newLine());
		valido=false;
		break;
		}

		return valido; 
	}
	private boolean validaCompuesto(TransferAtributo ta){
		boolean valido = true;
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_CHILDREN))+HTMLUtils.newLine());
		int numSubs=ta.getListaComponentes().size();
		switch (numSubs) {
		case 0: valido = false;
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+ 
				Lenguaje.getMensaje(Lenguaje.NO_SUBATTRIB)+ HTMLUtils.newLine());
		break;
		case 1: controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toYellowColor(Lenguaje.getMensaje(Lenguaje.WARNING)+": "))+
				Lenguaje.getMensaje(Lenguaje.ONE_SUBATTRIB)+HTMLUtils.newLine());
		break;
		}

		return valido;
	}


//	validacion de entidades
	private boolean validaEntidades(){
		DAOEntidades daoEntidades= new DAOEntidades(this.controlador.getPath());
		Vector <TransferEntidad> entidades =daoEntidades.ListaDeEntidades();
		boolean valido=true;
		int i=0;
		TransferEntidad t = new TransferEntidad();
		while (i<entidades.size()&& valido){
			t=entidades.elementAt(i);
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(Lenguaje.getMensaje(Lenguaje.RATIFYING)+" ")+HTMLUtils.toItalic(t.getNombre())+HTMLUtils.newLine());
			
			//por ahora validamos las claves y avisamos de si es padre de varias isA
			valido= validaKey(t)&&this.validaNombresAtributosEntidad(t);
			validaFidelidadEntidadEnIsA(t);
			i++;
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.newLine()+HTMLUtils.newLine());
		}

		return valido;
	}

//	metodos privados de validacion de entidades.	
	@SuppressWarnings("unchecked")
	private boolean validaKey(TransferEntidad te){
		DAOAtributos daoAtributos= new DAOAtributos(this.controlador.getPath());
		//valida si la entidad tiene clave y si esta dentro de sus atributos.
		//ademas si la entidad es debil, debe tener un atributo discriminante.
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_PRIMARYKEYS))+HTMLUtils.newLine());
		boolean valido=true;
		boolean noMulti = true;
		boolean compuesto=false;
		Vector atbs = te.getListaAtributos();
		Vector keys = te.getListaClavesPrimarias();
		int contador=0;
		TransferAtributo aux= new TransferAtributo();
		//int enIsA = entidadPerteneceAisA(te);
		Vector<int[]> resultados =entidadPerteneceAisA(te);
		int enIsA = 0;

		switch (resultados.size()){
		case 0: enIsA=-1; break; //no aparece
		case 1: enIsA= resultados.elementAt(0)[1]; break; //aparece una vez nos quedamos con lo que haya
		default:  //si aparece mas nos quedamos con la que tenga como padre
			for (int m=0;m<resultados.size();m++){
				if(resultados.elementAt(m)[1]==1) enIsA=1;
			}
		break;

		}

		// si no tiene clave primaria
		boolean relacionada = false;
		if (keys.isEmpty()&& enIsA<=0) {
			// Comprobar que no está asociada a ninguna relación
			
			DAORelaciones daoRelaciones= new DAORelaciones(this.controlador.getPath());
			Vector<TransferRelacion> rels = daoRelaciones.ListaDeRelaciones();
			int k = 0;
			
			while (k<rels.size() && !relacionada){
				Vector<EntidadYAridad> ents = rels.get(k).getListaEntidadesYAridades();
				int m = 0;
				while (!relacionada && m < ents.size()){
					relacionada = ents.get(m).getEntidad() == te.getIdEntidad();
					m++;
				}
				k++;
			}
			
			if (relacionada){
				if (te.isDebil()){
					controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
							Lenguaje.getMensaje(Lenguaje.NOKEY_WEAK_ENTITY)+HTMLUtils.newLine());
					valido=false;
				}else{ 
					controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+ 
							Lenguaje.getMensaje(Lenguaje.NOKEY_ENTITY_RELATION)+HTMLUtils.newLine());
					valido=false;
				}
			}
		}
		else
			// si tiene clave primaria, que esten dentro de sus atributos
			relacionada = true;
		
			if (!te.isDebil())
				if(!this.vectorEnterosContenidoEnVector(keys, atbs)) {
					//dos casos. que la clave sea un atbto compuesto o que no haya clave.
					//comprobamos que hay un atbto compuesto y si lo hay, lo comprobamos.
					while (contador<atbs.size()){
						aux.setIdAtributo(this.objectToInt(atbs.elementAt(contador)));
						aux=daoAtributos.consultarAtributo(aux);
						if(aux.getCompuesto()){
							if (compruebaClaveCompuesto(keys,aux)) compuesto=true; 

						}
						contador++;
					}
					if (compuesto) controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toYellowColor(Lenguaje.getMensaje(Lenguaje.WARNING)+": "))+
							Lenguaje.getMensaje(Lenguaje.ALL_CHILDREN_KEYS)+HTMLUtils.newLine()); 
					else{
						valido=false;
						controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
								Lenguaje.getMensaje(Lenguaje.NO_ATTRIB_KEY)+HTMLUtils.newLine());
					}
				}
				else{ // comprobamos que no haya una clave que sea un atributo multivalorado.
					while (contador<keys.size()&& noMulti){
						aux.setIdAtributo(this.objectToInt(keys.elementAt(contador)));
						aux=daoAtributos.consultarAtributo(aux);
						if(aux.isMultivalorado()){
							valido=false;
							noMulti=false;
							controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
								Lenguaje.getMensaje(Lenguaje.ATTRIBUTE)+" "+aux.getNombre()+Lenguaje.getMensaje(Lenguaje.MULTIVALUE_KEY)+HTMLUtils.newLine());
						}
						contador++;
					}
				}
		if (valido){
			//puede validarse por dos motivos:
			//1. si tiene clave 
			//2. si no la tiene y es hija en una relacion isA.
			//3. si no tiene clave, pero no está relacionada
			if (enIsA==1) 
				controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+ 
						Lenguaje.getMensaje(Lenguaje.ISA_PARENT)+HTMLUtils.newLine());
			else if (!relacionada)
				controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toYellowColor(Lenguaje.getMensaje(Lenguaje.WARNING)+": "))+ 
						Lenguaje.getMensaje(Lenguaje.NO_PRIMARY_KEY)+HTMLUtils.newLine());
			else
				controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+ 
						Lenguaje.getMensaje(Lenguaje.CORRECT_KEYS)+HTMLUtils.newLine());
		}

		return valido;
	}

	@SuppressWarnings("unchecked")
	private boolean compruebaClaveCompuesto(Vector clavesEntidad,TransferAtributo ta) {
		DAOAtributos daoAtributos= new DAOAtributos(this.controlador.getPath());
		int i=0;
		boolean todosBien=true;
		Vector subs = ta.getListaComponentes();
		TransferAtributo aux= new TransferAtributo();
		if (!ta.getCompuesto()){
			if (estaEnVectorDeEnteros(clavesEntidad, ta.getIdAtributo())) return true;
			else return false;
		}
		else{
			while (i<subs.size() && todosBien){
				aux.setIdAtributo(this.objectToInt(subs.elementAt(i)));
				aux=daoAtributos.consultarAtributo(aux);
				todosBien=todosBien && compruebaClaveCompuesto(clavesEntidad, aux);
				i++;
			}
			return todosBien;
		}	
	}
	private boolean validaNombresAtributosEntidad(TransferEntidad te){
		//comprueba que una entidad tenga atributos con nombres distintos.
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_ATTRIB_NAMES))+HTMLUtils.newLine());
		Vector<TransferAtributo> ats= this.dameAtributosEnTransfer(te.getListaAtributos());
		
		if (ats.size() < 1){
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+ 
					Lenguaje.getMensaje(Lenguaje.ENTITY)+" "+te.getNombre()+Lenguaje.getMensaje(Lenguaje.NO_ATTRIB)+HTMLUtils.newLine());
			return false;
		}
		
		TransferAtributo ti; //= new TransferAtributo();
		TransferAtributo tj; //= new TransferAtributo();
		int i=0;
		int j=1;
		boolean valido=true;
		while (i<ats.size() && valido){
			ti=ats.elementAt(i);
			while (j<ats.size() && valido){
				tj=ats.elementAt(j);
				if(ti.getNombre().toLowerCase().equals(tj.getNombre().toLowerCase())) {
					valido=false;
					controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+ 
							Lenguaje.getMensaje(Lenguaje.ATTRIB_NAME)+" "+tj.getNombre()+Lenguaje.getMensaje(Lenguaje.IS_REPEATED_IN_ENTITY)+te.getNombre()+"."+HTMLUtils.newLine());
				}

				j++;
			}
			i++;
			j=i+1;
		}
		if (valido)	
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+ 
					Lenguaje.getMensaje(Lenguaje.ATTRIB_NAMES)+" "+te.getNombre()+Lenguaje.getMensaje(Lenguaje.ARE_CORRECT)+HTMLUtils.newLine());
		return valido;
	}
	@SuppressWarnings("unchecked")
	private void validaFidelidadEntidadEnIsA(TransferEntidad te){
		DAORelaciones daoRelaciones= new DAORelaciones(this.controlador.getPath());
		Vector <TransferRelacion> relaciones =daoRelaciones.ListaDeRelaciones();
//		si la entidad es padre de una relacion isA comprueba que solo lo sea de una, sino, dara un aviso.
		int i=0;
		int papi=0;

		while (i<relaciones.size()){
			TransferRelacion tr=(TransferRelacion)relaciones.elementAt(i);
			if (tr.getTipo().equals("IsA")){
				Vector<EntidadYAridad> veya =tr.getListaEntidadesYAridades();
				if (!veya.isEmpty()&& veya.elementAt(0).getEntidad()==te.getIdEntidad()) papi++;
			}
			i++;
		}

		if (papi>1)	controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toYellowColor(Lenguaje.getMensaje(Lenguaje.WARNING)+": "))+
				Lenguaje.getMensaje(Lenguaje.ENT_PARENT_IN)+papi+Lenguaje.getMensaje(Lenguaje.ISA_RELATIONS)+"</p>");


	}


//	validacion de relaciones
	private boolean validaRelaciones(){
		DAORelaciones daoRelaciones= new DAORelaciones(this.controlador.getPath());
		Vector <TransferRelacion> relaciones =daoRelaciones.ListaDeRelaciones();
		boolean valido=true;
		int i=0;
		TransferRelacion t = new TransferRelacion();
		while (i<relaciones.size()&& valido){
			t=relaciones.elementAt(i);
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(Lenguaje.getMensaje(Lenguaje.RATIFYING))+HTMLUtils.toItalic(t.getNombre())+HTMLUtils.newLine());
			if(t.getTipo().equals("IsA"))
				valido= validaComponentesRelacionIsA(t);
			else if (t.getTipo().equals("Normal")) 
				valido=validaComponentesRelacionNormal(t);
			else 
				valido=validaComponentesRelacionDebil(t);
			i++;
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.newLine()+HTMLUtils.newLine());
		}

		return valido;		
	}

//	metodos privados de validacion de relaciones.

	@SuppressWarnings("unchecked")
	private boolean validaComponentesRelacionIsA(TransferRelacion tr){
		boolean valida=true;
		if(dameNumEntidadesDebiles(tr)>0){
			valida= false;
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.NO_WEAK_ENT_RELAT)+"</p>");
		}
		else{
			//controlador.mensajeDesde_SS(TC.SS_Validacion,"La relación "+tr.getNombre()+" es de tipo IsA");
			Vector<EntidadYAridad> veya =tr.getListaEntidadesYAridades();
			int tam =veya.size();
			switch (tam){
			case 0: controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+ 
					Lenguaje.getMensaje(Lenguaje.NO_PARENT_REL)+HTMLUtils.newLine());
			valida=false;
			break;
			case 1: controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.NO_CHILD_RELATION)+HTMLUtils.newLine());
			valida=false;
			break;
			default: {
				controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
						Lenguaje.getMensaje(Lenguaje.OK_RELATION)+HTMLUtils.newLine());
				break;
			}
			}
		}
		return valida;
	}

	@SuppressWarnings("unchecked")
	private boolean validaComponentesRelacionNormal(TransferRelacion tr){
		boolean valida=true;
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.THE_RELATION)+" "+tr.getNombre()+
				Lenguaje.getMensaje(Lenguaje.IS_NORMAL_TYPE))+HTMLUtils.newLine());
		if(dameNumEntidadesDebiles(tr)>0 && !this.misDebilesEstanEnDebiles(tr)){
			valida= false;
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.NO_WEAK_ENT_RELAT)+"</p>");
		}
		else{
			Vector<EntidadYAridad> veya =tr.getListaEntidadesYAridades();
			int tam =veya.size();
			switch (tam){
			case 0: controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.NO_ENT_RELATION)+HTMLUtils.newLine());
					valida = false;
			break;
			case 1: controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.ONE_ENT_REL)+HTMLUtils.newLine());
					valida = false;
			break;
			default: {
				controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
						Lenguaje.getMensaje(Lenguaje.MANY_ENT_REL)+HTMLUtils.newLine());
				break;
			}
			}
		}
		return valida;
	}

	@SuppressWarnings("unchecked")
	private boolean validaComponentesRelacionDebil(TransferRelacion tr){
		boolean valida=true;
		controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.THE_RELATION)+" "+tr.getNombre()+
				Lenguaje.getMensaje(Lenguaje.IS_WEAK_TYPE))+HTMLUtils.newLine());
		Vector<EntidadYAridad> veya =tr.getListaEntidadesYAridades();
		int tam =veya.size();
		int contD=0,contF=0;
		switch (tam){
		case 0: controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
				Lenguaje.getMensaje(Lenguaje.NO_ENT_RELATION)+HTMLUtils.newLine());
		valida=false;
		break;
		case 1: controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
				Lenguaje.getMensaje(Lenguaje.ONE_ENT_WEAK_REL)+HTMLUtils.newLine());
		valida=false;
		break;
		default:
			//en una relacion debil, necesitamos que haya como minimo una entidad fuerte y una debil.
			contD = this.dameNumEntidadesDebiles(tr);
		contF= tam-contD;
		}

		if(contD<1){
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.NO_WEAK_ENT_REL)+HTMLUtils.newLine());
			valida=false;
		}
		
		if (contD > 1){
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.MANY_WEAK_ENT_REL)+HTMLUtils.newLine());
			valida=false;
		}
		
		if(contF<1){
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
					Lenguaje.getMensaje(Lenguaje.NO_STRONG_ENT_REL)+HTMLUtils.newLine());
			valida=false;
		}
		
		if(contD == 1 && contF <= 1)
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
					Lenguaje.getMensaje(Lenguaje.OK_ENT_REL)+HTMLUtils.newLine());

		return valida;
	}
	
	@SuppressWarnings("unchecked")
	private boolean validaDominios(){
		DAODominios daoDominios= new DAODominios(this.controlador.getPath());
		Vector <TransferDominio> dominios = daoDominios.ListaDeDominios();
		boolean valido=true;
		int i=0;
		TransferDominio t;
		while (i<dominios.size()&& valido){
			t=dominios.get(i);
			controlador.mensajeDesde_SS(TC.SS_Validacion,
					HTMLUtils.toBold(Lenguaje.getMensaje(Lenguaje.RATIFYING))+HTMLUtils.toItalic(t.getNombre())+
					HTMLUtils.newLine());

			// Validar que es único
			controlador.mensajeDesde_SS(TC.SS_Validacion,
					HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_DOMAIN))+HTMLUtils.newLine());
			boolean encontrado = false;
			int j = i+1;
			while (!encontrado && j<dominios.size()){
				encontrado = t.getNombre().equals(dominios.get(j).getNombre());
				j++;
			}
			
			if (encontrado){
				valido=false;
				controlador.mensajeDesde_SS(TC.SS_Validacion, 
						HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
						Lenguaje.getMensaje(Lenguaje.REPEATED_DOM_NAMES)+HTMLUtils.newLine());				
			}else{
				controlador.mensajeDesde_SS(TC.SS_Validacion,
						HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
						Lenguaje.getMensaje(Lenguaje.OK_DOMAIN)+HTMLUtils.newLine());
			}
			
			// Validar que tiene valores y son distintos
			controlador.mensajeDesde_SS(TC.SS_Validacion,
					HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_DOMAIN_VALUES))+HTMLUtils.newLine());
			Vector<String> valores = t.getListaValores();
			
			if (valores == null || valores.size() < 1){
				valido=false;
				controlador.mensajeDesde_SS(TC.SS_Validacion, 
						HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
						Lenguaje.getMensaje(Lenguaje.NO_VALUE_DOM)+HTMLUtils.newLine());
			}else{
				String valorComprobado = null;
				int k = 0;
				boolean seRepite = false;
				while (k < valores.size() && !seRepite){
					valorComprobado = valores.get(k);
					int m = k + 1;
					while (m < valores.size() && !seRepite){
						seRepite = (valorComprobado.equals(valores.get(m)));
						m++;
					}
					k++;
				}
				
				if (seRepite){
					valido=false;
					controlador.mensajeDesde_SS(TC.SS_Validacion, 
							HTMLUtils.toBold(HTMLUtils.toRedColor(Lenguaje.getMensaje(Lenguaje.ERROR)+": "))+
							Lenguaje.getMensaje(Lenguaje.THE_VALUE) + valorComprobado +Lenguaje.getMensaje(Lenguaje.IS_REPEATED)+HTMLUtils.newLine());
				}else{
					controlador.mensajeDesde_SS(TC.SS_Validacion,
							HTMLUtils.toBold(HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
							Lenguaje.getMensaje(Lenguaje.OK_DOM_VALUES)+HTMLUtils.newLine());
				}
			}
			
			// Comprobar si se usa (esto sólo da un aviso si falla)
			controlador.mensajeDesde_SS(TC.SS_Validacion, 
					HTMLUtils.toItalic(Lenguaje.getMensaje(Lenguaje.RATIFYING_DOM_USE))+HTMLUtils.newLine());
			
			DAOAtributos daoAtributos= new DAOAtributos(this.controlador.getPath());
			Vector <TransferAtributo> atributos =daoAtributos.ListaDeAtributos();
			boolean esta=false;
			int k=0;
			while(!esta && k<atributos.size()){
				esta = atributos.get(k).getDominio().equalsIgnoreCase(t.getNombre());
				k++;
			}
			
			if (esta){
				controlador.mensajeDesde_SS(TC.SS_Validacion, HTMLUtils.toBold(
								HTMLUtils.toGreenColor(Lenguaje.getMensaje(Lenguaje.SUCCESS)+": "))+
								Lenguaje.getMensaje(Lenguaje.USE_DOM) + HTMLUtils.newLine());
			}else {
				controlador.mensajeDesde_SS(TC.SS_Validacion, HTMLUtils.toBold(
						HTMLUtils.toYellowColor(Lenguaje.getMensaje(Lenguaje.WARNING)+": "))+
						Lenguaje.getMensaje(Lenguaje.NO_USE_DOM) + HTMLUtils.newLine());
			}
			
			// Incrementar contador
			i++;
			controlador.mensajeDesde_SS(TC.SS_Validacion,HTMLUtils.newLine()+HTMLUtils.newLine());
		}

		return valido;
	}

	//metodo principal
	public void validaBaseDeDatos(){
		boolean fenomeno=false;
		//un fallo en la validacion detiene el proceso
		controlador.mensajeDesde_SS(TC.SS_Validacion,"<p align=\"center\"><b><font size=\"5\">"+Lenguaje.getMensaje(Lenguaje.RATIFYING_DB)+"</font></b></p>");
		
		controlador.mensajeDesde_SS(TC.SS_Validacion,"<p><b>"+Lenguaje.getMensaje(Lenguaje.RATIFYING_DOMAINS)+"</b></p>");
		controlador.mensajeDesde_SS(TC.SS_Validacion,"<hr>");
		if (this.validaDominios()){
			controlador.mensajeDesde_SS(TC.SS_Validacion,"<p>&nbsp;</p>");
			controlador.mensajeDesde_SS(TC.SS_Validacion,"<p><b>"+Lenguaje.getMensaje(Lenguaje.RATIFYING_ATTRIBUTES)+"</b></p>");
			controlador.mensajeDesde_SS(TC.SS_Validacion,"<hr>");
			if (this.validaAtributos()){
				controlador.mensajeDesde_SS(TC.SS_Validacion,"<p>&nbsp;</p>");
				controlador.mensajeDesde_SS(TC.SS_Validacion,"<p><b>"+Lenguaje.getMensaje(Lenguaje.RATIFYING_ENTITIES)+"</b></p>");
				controlador.mensajeDesde_SS(TC.SS_Validacion,"<hr>");
				if (this.validaEntidades()){
					controlador.mensajeDesde_SS(TC.SS_Validacion,"<p>&nbsp;</p>");
					controlador.mensajeDesde_SS(TC.SS_Validacion,"<p><b>"+Lenguaje.getMensaje(Lenguaje.RATIFYING_RELATIONS)+"</b></p>");
					controlador.mensajeDesde_SS(TC.SS_Validacion,"<hr>");
					if (this.validaRelaciones()) fenomeno=true;
				} // validaEntidades
			} // ValidaAtributos
		} // ValidaDominios
		
		// Recordar si la validación fue correcta
		modeloValidado = fenomeno;
		
		// Mostrar el texto
		controlador.mensajeDesde_SS(TC.SS_Validacion,"<p>&nbsp;</p>");
		if (fenomeno) 	controlador.mensajeDesde_SS(TC.SS_Validacion,"<p align=\"center\"><b><font color=\"#008000\" size=\"5\">"+Lenguaje.getMensaje(Lenguaje.RATIFY_SUCCESS)+"</font></b></p>");
		else 	controlador.mensajeDesde_SS(TC.SS_Validacion,"<p align=\"center\"><font size=\"5\" color=\"#FF0000\"><b>"+Lenguaje.getMensaje(Lenguaje.RATIFY_ERROR)+"</b></font></p>");
	}




//	metodos auxiliares

	private String quitaParenDominio(String dominio){
		int c=dominio.indexOf("(");
		return dominio.substring(0,c);
	}

	@SuppressWarnings("unchecked")
	private boolean estaEnVectorDeEnteros(Vector sinParam, int valor){
		int i=0;
		boolean encontrado=false;
		int elem=0;
		while(i<sinParam.size() && !encontrado){
			elem= this.objectToInt(sinParam.elementAt(i));
			if(elem==valor) encontrado = true;
			i++;
		}
		return encontrado;
	}

	@SuppressWarnings("unchecked")
	private boolean vectorEnterosContenidoEnVector(Vector subVector, Vector vector){
		// comprueba si un vector de enteros esta dentro de otro si estos no estan parametrizados (no funciona contains)
		boolean esta=false;
		boolean contiene=true;
		for (int i =0; i<subVector.size();i++){
			esta=false;
			for (int j=0;j<vector.size();j++)
				if (objectToInt(subVector.elementAt(i))==objectToInt(vector.elementAt(j))) esta=true;
			contiene= esta && contiene;
		}
		return contiene;
	}

	//private int entidadPerteneceAisA(TransferEntidad te){
	@SuppressWarnings("unchecked")
	private Vector<int[]> entidadPerteneceAisA(TransferEntidad te){
		DAORelaciones daoRelaciones= new DAORelaciones(this.controlador.getPath());
		Vector <TransferRelacion> relaciones =daoRelaciones.ListaDeRelaciones();
		/*lo que hay que tener en cuenta para ver si una entidad pertenece a una relacion isA (como hija) segun nuestro
		 diseño es que sea como minimo la segunda de la lista de entidades y aridades de la relacion. Esto es porque 
		 la primera indica que es el padre y este si tiene que tener clave.
		 El metodo devuelve lo siguiente:
		 en la primera componente: 
		 la relacion isA en la que actua.
		 en la segunda componente: 
		 si no pertenece a una isA -> -1
		 sino: si es padre -> 0
		 si es hija -> 1
		 */

		Vector<int[]> resultados = new Vector<int[]>();

		int i=0;
		int j=0;
		//	int queSoy=-1;
		//while (i<relaciones.size()&&!perteneceAisA){
		while (i<relaciones.size()){
			TransferRelacion tr=(TransferRelacion)relaciones.elementAt(i);
			Vector<EntidadYAridad> veya =tr.getListaEntidadesYAridades();
			int[] parejaResultados = new int [2];
			parejaResultados[0]=tr.getIdRelacion();
			while (j<veya.size()){
				EntidadYAridad eya = veya.elementAt(j);
				//if(eya.getEntidad()==te.getIdEntidad()){
				if(eya.getEntidad()==te.getIdEntidad()&& tr.getTipo().equals("IsA")){
					//	encontrada=true;
					if(j==0){
						parejaResultados[1]=0;
						resultados.add(parejaResultados);
						//	queSoy=0;
					}
					else{
						parejaResultados[1]=1;
						resultados.add(parejaResultados);
						//	queSoy=1;
					}
				}
				j++;
			}
			//if (encontrada && tr.getTipo().equals("IsA")) {
			//perteneceAisA = true;

			//	}
			j=0;
			i++;
		}
		//if (!perteneceAisA) queSoy=-1;

		//return queSoy;
		return resultados;
	}

	@SuppressWarnings("unchecked")
	private int dameNumEntidadesDebiles (TransferRelacion tr){
		DAOEntidades daoEntidades= new DAOEntidades(this.controlador.getPath());
		TransferEntidad aux = new TransferEntidad();
		Vector<EntidadYAridad> veya =tr.getListaEntidadesYAridades();
		int cont=0;
		for (int k =0;k<veya.size();k++){
			aux.setIdEntidad(veya.elementAt(k).getEntidad());
			aux=daoEntidades.consultarEntidad(aux);
			if (aux.isDebil()) cont++;
		}
		return cont;
	}

	@SuppressWarnings("unchecked")
	private boolean misDebilesEstanEnDebiles(TransferRelacion rel){
		DAORelaciones daoRelaciones = new DAORelaciones(this.getControlador().getPath());
		DAOEntidades daoEntidades = new DAOEntidades(this.getControlador().getPath());
		Vector<TransferRelacion> relaciones= daoRelaciones.ListaDeRelaciones();
		boolean enDebil=false;
		boolean encontrada=false;
		TransferEntidad entDebil= new TransferEntidad();
		int k=0;
		Vector<EntidadYAridad> veyaRel=rel.getListaEntidadesYAridades();
		while(k<veyaRel.size()&&!encontrada){
			EntidadYAridad eya = veyaRel.elementAt(k);
			entDebil.setIdEntidad(eya.getEntidad());
			entDebil=daoEntidades.consultarEntidad(entDebil);
			if(entDebil.isDebil()) encontrada=true;
			k++;
		}


		int i=0;
		int j=0;
		while(i<relaciones.size()&& !enDebil){
			TransferRelacion tr=relaciones.elementAt(i);
			Vector<EntidadYAridad> veya=tr.getListaEntidadesYAridades();
			j=0;
			while(j<veya.size()&&!enDebil){
				EntidadYAridad eya = veya.elementAt(j);
				if (eya.getEntidad()==entDebil.getIdEntidad() && tr.getTipo().equals("Debil")) enDebil=true;
				j++;
			}
			i++;
		}

		return enDebil;	
	}




//	metodos de recorrido de los daos para la creacion de las tablas.
	@SuppressWarnings("unchecked")
	private void generaTablasEntidades(){
		DAOEntidades daoEntidades= new DAOEntidades(controlador.getPath());
		Vector<TransferEntidad> entidades= daoEntidades.ListaDeEntidades();

		//recorremos las entidades generando las tablas correspondientes.
		for (int i=0;i<entidades.size();i++){
			Vector<TransferAtributo>multivalorados=new Vector<TransferAtributo>();
			TransferEntidad te=entidades.elementAt(i);
			Tabla tabla = new Tabla(te.getNombre());
			Vector<TransferAtributo> atribs=this.dameAtributosEnTransfer(te.getListaAtributos());
			
			//recorremos los atributos añadiendolos a la tabla
			for (int j=0;j<atribs.size();j++){
				TransferAtributo ta=atribs.elementAt(j);
				if (ta.getCompuesto()) tabla.añadeListaAtributos(this.atributoCompuesto(ta,
															te.getNombre(),""),tiposEnumerados);
				else if (ta.isMultivalorado()) multivalorados.add(ta);
				else tabla.añadeAtributo(ta.getNombre(), ta.getDominio(),te.getNombre(),
												tiposEnumerados, ta.getUnique(), ta.getNotnull());
			}
			
			// Añadimos las claves a la relación
			
			
			//añadimos las claves primarias o logeneraTablasEntidadess discriminantes si la entidad es debil.
			Vector<TransferAtributo> claves=this.dameAtributosEnTransfer(te.getListaClavesPrimarias());
			for (int c=0;c<claves.size();c++){
				TransferAtributo ta=claves.elementAt(c);
				if (ta.isMultivalorado()) multivalorados.add(ta);
				else
					if (ta.getCompuesto())
						tabla.añadeListaClavesPrimarias(this.atributoCompuesto(ta,te.getNombre(),""));
					else 
						//si es normal, lo añadimos como clave primaria.
						tabla.añadeClavePrimaria(ta.getNombre(),ta.getDominio(),te.getNombre());

			}
			//añadimos a las tablas del sistema.
			tablasEntidades.put(te.getIdEntidad(),tabla);
			//tratamos los multivalorados que hayan surgido en el proceso.
			for(int mul=0;mul<multivalorados.size();mul++){
				TransferAtributo multi=multivalorados.elementAt(mul);
				this.atributoMultivalorado(multi, te.getIdEntidad());
			}
			
			// Establecimiento de uniques
			Vector<String> listaUniques = te.getListaUniques();
			for (int m = 0; m < listaUniques.size(); m++){
				tabla.getUniques().add(listaUniques.get(m));
			}
		}

	}
	
	@SuppressWarnings("unchecked")
	private void generaTablasRelaciones() {
		DAORelaciones daoRelaciones = new DAORelaciones(controlador.getPath());
		Vector<TransferRelacion> relaciones = daoRelaciones.ListaDeRelaciones();

		// recorremos las relaciones creando sus tablas, en funcion de su tipo.
		for (int i = 0; i < relaciones.size(); i++) {
			TransferRelacion tr = relaciones.elementAt(i);
			Vector<TransferAtributo> multivalorados = new Vector<TransferAtributo>();
			// si es una relacion normal, añadiremos los atributos propios y las
			// claves de las entidades implicadas.
			Vector<EntidadYAridad> veya = tr.getListaEntidadesYAridades();

			if (tr.getTipo().equalsIgnoreCase("Normal")) {
				// creamos la tabla
				Tabla tabla = new Tabla(tr.getNombre());

				// añadimos los atributos propios.
				Vector<TransferAtributo> ats = this.dameAtributosEnTransfer(tr
						.getListaAtributos());
				for (int a = 0; a < ats.size(); a++) {
					TransferAtributo ta = ats.elementAt(a);
					if (ta.getCompuesto())
						tabla.añadeListaAtributos(this.atributoCompuesto(ta, tr
								.getNombre(), ""), tiposEnumerados);
					else if (ta.isMultivalorado())
						multivalorados.add(ta);
					else
						tabla.añadeAtributo(ta.getNombre(), ta.getDominio(), tr
								.getNombre(), tiposEnumerados, ta.getUnique(), ta.getNotnull());
				}

				// TRATAMIENTO DE ENTIDADES
				// Comprobar si todas las entidades están con relación 0..1 o 1..1
				boolean soloHayUnos = true;
				int k = 0;
				while (soloHayUnos && k<veya.size()){
					EntidadYAridad eya = veya.get(k);
					if (eya.getFinalRango() <= 1){
						k++;
					}else{
						soloHayUnos = false;
					}
				}
				
				// Para cada entidad...
				boolean esLaPrimeraDel1a1 = true;
				for (int m = 0; m < veya.size(); m++){
					// Añadir su clave primaria a la relación (es clave foránea)
					EntidadYAridad eya = veya.elementAt(m);
					Tabla ent = tablasEntidades.get(eya.getEntidad());
					
					Vector<String[]> previasPrimarias;
					if (ent.getPrimaries().isEmpty()){
						previasPrimarias = ent.getAtributos();
					}else{
						previasPrimarias = ent.getPrimaries();
					}
					
					// ...pero antes renombrarla con el rol
					Vector<String[]> primarias = new Vector<String[]>();
					String[] referenciadas = new String[previasPrimarias.size()];
					for (int q=0; q<previasPrimarias.size(); q++){
						String[] clave = new String[3];
						if (!eya.getRol().equals("")){
							clave[0] = eya.getRol() + "_" + previasPrimarias.get(q)[0];
						}else{
							clave[0] = previasPrimarias.get(q)[0];
						}
						clave[1] = previasPrimarias.get(q)[1];
						clave[2] = previasPrimarias.get(q)[2];
						
						primarias.add(clave);
						
						referenciadas[q] = previasPrimarias.get(q)[0];
					}
					
					tabla.añadeListaAtributos(primarias,
							tiposEnumerados);
					tabla.añadeListaClavesForaneas(primarias, ent.getNombreTabla(), referenciadas);
					
					// Si es 0..n poner como clave
					if (eya.getFinalRango() != 1)
						tabla.añadeListaClavesPrimarias(primarias);
					else{
						if (soloHayUnos && esLaPrimeraDel1a1){
							tabla.añadeListaClavesPrimarias(primarias);
							esLaPrimeraDel1a1 = false;
						}else if (soloHayUnos){
							String uniques = "";
							for (int q = 0; q < primarias.size(); q++){
								if (q == 0){
									uniques += primarias.get(q)[0];
								}else{
									uniques += ", " + primarias.get(q)[0];
								}
							}
							uniques += "#" + ent.getNombreTabla();
							
							tabla.getUniques().add(uniques);
						}
					}
				}
				
				// -------------------------------------------
				
				tablasRelaciones.put(tr.getIdRelacion(), tabla);
				for (int mul = 0; mul < multivalorados.size(); mul++) {
					TransferAtributo multi = multivalorados.elementAt(mul);
					this.atributoMultivalorado(multi, tr.getIdRelacion());
				}
			}

			// si no es normal
			else
			// si es del tipo IsA, actualizamos añadiendo la clave del padre a
			// las tablas hijas.
			if (tr.getTipo().equals("IsA")) {

				/*
				 * recorremos todas las entidades asociadas a la relacion.
				 * sabemos ademas, por criterios del diseño, que la primera
				 * entidad es siempre padre.
				 */
				EntidadYAridad padre = veya.firstElement();
				for (int e = 1; e < veya.size(); e++) {
					EntidadYAridad hija = veya.elementAt(e);
					// añadimos la informacion de clave a las tablas hijas,
					// buscandolas en el sistema.
					tablasEntidades.get(hija.getEntidad()).añadeListaAtributos(
							tablasEntidades.get(padre.getEntidad())
									.getPrimaries(), tiposEnumerados);
					
					tablasEntidades.get(hija.getEntidad())
							.añadeListaClavesPrimarias(
									tablasEntidades.get(padre.getEntidad())
											.getPrimaries());
					
					Vector<String[]> clavesPadre = tablasEntidades.get(padre.getEntidad()).
																			getPrimaries();
					String[] referenciadas = new String[clavesPadre.size()];
					for (int q=0; q<clavesPadre.size(); q++){
						referenciadas[q] = clavesPadre.get(q)[0];
					}
					
					tablasEntidades.get(hija.getEntidad())
					.añadeListaClavesForaneas(
							tablasEntidades.get(padre.getEntidad())
									.getPrimaries(), 
							tablasEntidades.get(padre.getEntidad())
									.getNombreTabla(),
							referenciadas);
				}

			}
			// si es de tipo debil
			else {
				/*
				 * buscamos la entidad debil, que ya tiene tabla y le añadimos
				 * los atributos de las entidades fuertes de las que dependa.
				 * Ademas los pondremos como claves foraneas. Contaremos, las
				 * entidades fuertes y las debiles que aparezcan, pues este sera
				 * el criterio a seguir a la hora de reasignar las claves.
				 */
				DAOEntidades daoEntidades = new DAOEntidades(controlador
						.getPath());
				Vector<TransferEntidad> fuertes = new Vector<TransferEntidad>();
				Vector<TransferEntidad> debiles = new Vector<TransferEntidad>();
				for (int s = 0; s < veya.size(); s++) {
					TransferEntidad aux = new TransferEntidad();
					EntidadYAridad eya = veya.elementAt(s);
					aux.setIdEntidad(eya.getEntidad());
					aux = daoEntidades.consultarEntidad(aux);
					if (aux.isDebil())
						debiles.add(aux);
					else
						fuertes.add(aux);
				}
				// ahora recorremos las fuertes, sacando sus claves y
				// metiendolas en las debiles.
				for (int f = 0; f < fuertes.size(); f++) {
					TransferEntidad fuerte = fuertes.elementAt(f);
					Tabla tFuerte = tablasEntidades.get(fuerte.getIdEntidad());
					for (int d = 0; d < debiles.size(); d++) {
						TransferEntidad debil = debiles.elementAt(d);
						Tabla tDebil = tablasEntidades
								.get(debil.getIdEntidad());
						tDebil.añadeListaAtributos(tFuerte.getPrimaries(),
								tiposEnumerados);
						
						Vector<String[]> clavesFuerte = tFuerte.getPrimaries();
						String[] referenciadas = new String[clavesFuerte.size()];
						for (int q=0; q<clavesFuerte.size(); q++){
							referenciadas[q] = clavesFuerte.get(q)[0];
						}
						tDebil.añadeListaClavesForaneas(tFuerte.getPrimaries(),
								tFuerte.getNombreTabla(), referenciadas);
					}

				}
			}

		}

	}
	
	@SuppressWarnings("unchecked")
	private void generaTiposEnumerados(){
		DAODominios daoDominios= new DAODominios(controlador.getPath());
		Vector<TransferDominio> dominios = daoDominios.ListaDeDominios();

		//recorremos los dominios creando sus tipos enumerados
		for (int i=0;i<dominios.size();i++){
			TransferDominio td=dominios.elementAt(i);
			
			Enumerado enu = new Enumerado(td.getNombre());
			
			// Obtener todos sus posibles valores
			Vector<String> valores = td.getListaValores();
			for (int k=0; k<valores.size(); k++){
				enu.anadeValor(valores.get(k));
			}
			
			// Insertar en la tabla Hash
			tiposEnumerados.put(td.getIdDominio(), enu);
		}
	}

	public void reset(){
		tablasEntidades.clear();
		tablasRelaciones.clear();
		tablasMultivalorados.clear();
		tiposEnumerados.clear();
		
		modeloValidado = false;
		conexionScriptGenerado = null;
		sql="";
		sqlHTML="";
	}

	public void generaScriptSQL(TransferConexion conexion){
		if (!modeloValidado){
			JOptionPane.showMessageDialog(null,
					Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
					Lenguaje.getMensaje(Lenguaje.MUST_RATIFY_MODEL),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.PLAIN_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			return;
		}
		
		// Eliminar tablas anteriores, pero recordar que el modelo sí ha sido validado
		reset();
		modeloValidado = true;
		conexionScriptGenerado = conexion;
		
		// Cabeceras de los documentos
		sql="-- SCRIPT GENERATED BY DATABASE DESIGN TOOL\n";
		sql+="-- Script generated using " + conexion.getRuta() + " syntax \n"; 
		sqlHTML="<p align=\"center\"><b><font size=\"5\">"+Lenguaje.getMensaje(Lenguaje.DESIGN_SQL_CODE)+" (" + 
					conexion.getRuta() + ")</font></b></p>";
	
		// Creamos las tablas
		this.generaTiposEnumerados();
		this.generaTablasEntidades();
		this.generaTablasRelaciones();
		//sacamos el codigo de cada una de ellas recorriendo las hashtables e imprimiendo.
		this.creaTablas(conexion);
		this.creaEnums(conexion);
		this.ponClaves(conexion);	
		this.ponRestricciones();
		controlador.mensajeDesde_SS(TC.SS_GeneracionScriptSQL,sqlHTML);
	}

	public void generaFicheroSQL(){
		// Si no se ha generado antes el script lanzamos un error
		if (sql.isEmpty()){
			JOptionPane.showMessageDialog(null,
					Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
					Lenguaje.getMensaje(Lenguaje.MUST_GENERATE_SCRIPT),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.PLAIN_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			return;
		}
		// Si ya se ha generado el Script
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle(Lenguaje.getMensaje(Lenguaje.DBCASE));
		jfc.setFileFilter(new FileNameExtensionFilter(Lenguaje.getMensaje(Lenguaje.SQL_FILES), "sql"));
		int resul = jfc.showSaveDialog(null);
		if (resul == 0){
			File ruta = jfc.getSelectedFile();
			try {
				FileWriter file = new FileWriter(ruta);
				file.write(sql);
				file.close();

				JOptionPane.showMessageDialog(
						null,
						Lenguaje.getMensaje(Lenguaje.INFO)+"\n"+    
						Lenguaje.getMensaje(Lenguaje.OK_FILE)+"\n" +
						Lenguaje.getMensaje(Lenguaje.FILE)+": "+ruta,
						Lenguaje.getMensaje(Lenguaje.DBCASE),
						JOptionPane.PLAIN_MESSAGE,
						new ImageIcon(getClass().getClassLoader().getResource(ImagePath.OK)));

			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
						Lenguaje.getMensaje(Lenguaje.SCRIPT_ERROR),
						Lenguaje.getMensaje(Lenguaje.DBCASE),
						JOptionPane.PLAIN_MESSAGE,
						new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			}	
		}
	}
	
	public Vector<TransferConexion> obtenerTiposDeConexion(){
		Vector<String> nombres = FactoriaConectores.obtenerTodosLosConectores();
		
		Vector<TransferConexion> conexiones;
		conexiones = new Vector<TransferConexion>();
		conexiones.clear();
		for (int i = 0; i < nombres.size(); i++){
			conexiones.add(new TransferConexion(i, nombres.get(i)));
		}
		
		return conexiones;
	}

	public void ejecutarScriptEnDBMS(TransferConexion tc) {

		// Comprobaciones previas
		if (tc.getTipoConexion() != conexionScriptGenerado.getTipoConexion()) {
			int respuesta = JOptionPane.showConfirmDialog(null,
					Lenguaje.getMensaje(Lenguaje.WARNING)+".\n" +
					Lenguaje.getMensaje(Lenguaje.SCRIPT_GENERATED_FOR)+": \n" +
					"     " + conexionScriptGenerado.getRuta() + " \n" +
					Lenguaje.getMensaje(Lenguaje.CONEXION_TYPE_IS)+": \n" + 
					"     " + tc.getRuta() + "\n" +
					Lenguaje.getMensaje(Lenguaje.POSSIBLE_ERROR_SRIPT)+" \n" +
					Lenguaje.getMensaje(Lenguaje.SHOULD_GENERATE_SCRIPT)+" \n" +
					Lenguaje.getMensaje(Lenguaje.OF_CONEXION)+"\n"+
					Lenguaje.getMensaje(Lenguaje.CONTINUE_ANYWAY),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.PREGUNTA)));
			if (respuesta == JOptionPane.CANCEL_OPTION){
				return;	
			}
		}
		
		// Ejecutar en DBMS
		System.out.println("Datos de conexión a la base de datos");
		System.out.println("------------------------------------");
		System.out.println("DBMS: " + tc.getRuta() + "(" + tc.getTipoConexion() + ")");
		System.out.println("Usuario: " + tc.getUsuario());
		// System.out.println("Password: " + tc.getPassword());
		
		System.out.println("Intentando conectar...");
		ConectorDBMS conector = FactoriaConectores.obtenerConector(tc.getTipoConexion());
		try {
			conector.abrirConexion(tc.getRuta(), tc.getUsuario(), tc.getPassword());
		} catch (SQLException e) {
			// Avisar por consola
			System.out.println("ERROR: No se pudo abrir una conexión con la base de datos");
			System.out.println("MOTIVO");
			System.out.println(e.getMessage());
			
			// Avisar por GUI
			JOptionPane.showMessageDialog(null,
					Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
					Lenguaje.getMensaje(Lenguaje.NO_DB_CONEXION)+" \n" +
					Lenguaje.getMensaje(Lenguaje.REASON)+": \n" + e.getMessage(),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.PLAIN_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			
			// Terminar
			return;
		}
//		System.out.println("Conectado correctamente.");
		
//		System.out.println("Ejecutando script...");
		String ordenActual = null;
		try {
			// Crear la base de datos
			conector.usarDatabase(tc.getDatabase());
			
			// Ejecutar cada orden
			String[] orden = sql.split(";");
			for (int i=0; i < orden.length; i++){
				if ((orden[i] != null) && (!orden[i].equals("")) && (!orden[i].equals("\n"))){
					ordenActual = orden[i].trim() + ";";
					
					// Eliminar los comentarios y lineas en blanco
					if (ordenActual.startsWith("--") && !ordenActual.contains("\n")){
						continue;
					}
					
					while (ordenActual.startsWith("--") || ordenActual.startsWith("\n")){
						ordenActual = ordenActual.substring(ordenActual.indexOf("\n") + 1);
					}
					
					// Ejecutar la orden
					conector.ejecutarOrden(ordenActual);	
				}
			}
		} catch (SQLException e) {
//			// Avisar por consola
//			System.out.println("ERROR: No se pudo ejecutar el script");
//			System.out.println("SENTENCIA");
//			System.out.println(ordenActual);
//			System.out.println("MOTIVO");
//			System.out.println(e.getMessage());
			
			// Avisar por GUI
			JOptionPane.showMessageDialog(null,
					Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
					Lenguaje.getMensaje(Lenguaje.CANT_EXECUTE_SCRIPT)+" \n" +
					Lenguaje.getMensaje(Lenguaje.ENQUIRY_ERROR)+": \n" + ordenActual + "\n" + 
					Lenguaje.getMensaje(Lenguaje.REASON)+": \n" + e.getMessage(),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.PLAIN_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			
			// Terminar
			return;
		}
//		System.out.println("Ejecutado correctamente.");
		
//		System.out.println("Cerrando conexión");
		try {
			conector.cerrarConexion();
		} catch (SQLException e) {
//			// Avisar por consola
//			System.out.println("ERROR: No se pudo cerrar la conexión");
//			System.out.println("MOTIVO");
//			System.out.println(e.getMessage());
			
			// Avisar por GUI
			JOptionPane.showMessageDialog(null,
					Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
					Lenguaje.getMensaje(Lenguaje.CANT_CLOSE_CONEXION)+" \n" +
					Lenguaje.getMensaje(Lenguaje.REASON)+" \n" + e.getMessage(),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.PLAIN_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			
			// Terminar
			return;
		}
		System.out.println("Conexión cerrada correctamente");
		
		JOptionPane.showMessageDialog(null,
				Lenguaje.getMensaje(Lenguaje.INFO)+"\n" +
				Lenguaje.getMensaje(Lenguaje.OK_SCRIPT_EXECUT),
				Lenguaje.getMensaje(Lenguaje.DBCASE),
				JOptionPane.PLAIN_MESSAGE,
				new ImageIcon(getClass().getClassLoader().getResource(ImagePath.OK)));
	}
	
	@SuppressWarnings("unchecked")
	private void creaTablas(TransferConexion conexion){
		sqlHTML+="<p><b>"+Lenguaje.getMensaje(Lenguaje.TABLES_SECTION)+"</b></p><hr>";
		sql+="\n-- "+Lenguaje.getMensaje(Lenguaje.TABLES_SECTION)+"\n";

		Iterator tablasM=tablasMultivalorados.iterator();
		while (tablasM.hasNext()){
			Tabla t =(Tabla)tablasM.next();
			sqlHTML+=t.codigoHTMLCreacionDeTabla(conexion);
			sql+=t.codigoEstandarCreacionDeTabla(conexion);
		}
	
		Iterator tablasR=tablasRelaciones.values().iterator();
		while (tablasR.hasNext()){
			Tabla t =(Tabla)tablasR.next();
			sqlHTML+=t.codigoHTMLCreacionDeTabla(conexion);
			sql+=t.codigoEstandarCreacionDeTabla(conexion);
		}
		
		String tablasEntidad = "";
		String tablasEntidadHTML = "";
//		Iterator tablasE=tablasEntidades.values().iterator();
//		while (tablasE.hasNext()){
//			Tabla t =(Tabla)tablasE.next();
//			sqlHTML+=t.codigoHTMLCreacionDeTabla(conexion);
//			sql+=t.codigoEstandarCreacionDeTabla(conexion);
//		}
		
		Iterator tablasE=tablasEntidades.values().iterator();
		while (tablasE.hasNext()){
			Tabla t =(Tabla)tablasE.next();
			if (esPadreEnIsa(t)){
				tablasEntidadHTML = t.codigoHTMLCreacionDeTabla(conexion) + tablasEntidadHTML;
				tablasEntidad = t.codigoEstandarCreacionDeTabla(conexion) + tablasEntidad;
			}else{
				tablasEntidadHTML+=t.codigoHTMLCreacionDeTabla(conexion);
				tablasEntidad+=t.codigoEstandarCreacionDeTabla(conexion);
			}
		}
		
		sql += tablasEntidad;
		sqlHTML += tablasEntidadHTML;
	}
	
	@SuppressWarnings("unchecked")
	private boolean esPadreEnIsa(Tabla tabla){
		boolean encontrado = false;
		
		DAORelaciones daoRelaciones = new DAORelaciones(controlador.getPath());
		Vector<TransferRelacion> relaciones = daoRelaciones.ListaDeRelaciones();

		// recorremos las relaciones buscando las isa
		int i = 0;
		while (i < relaciones.size() && !encontrado) {
			TransferRelacion tr = relaciones.elementAt(i);
			
			if (tr.getTipo().equals("IsA")) {
				// Obtener ID del padre
				Vector<EntidadYAridad> veya = tr.getListaEntidadesYAridades();
				int  idPadre = veya.firstElement().getEntidad();
				
				DAOEntidades daoEntidades= new DAOEntidades(controlador.getPath());
				TransferEntidad te = new TransferEntidad();
				te.setIdEntidad(idPadre);
				te = daoEntidades.consultarEntidad(te);
				
				Tabla t = new Tabla(te.getNombre());
				t = t.creaClonSinAmbiguedadNiEspacios();
				
				encontrado = t.getNombreTabla().equalsIgnoreCase(tabla.getNombreTabla());
			}
			
			i++;
		}
		
		return encontrado;
	}
	
	private void creaEnums(TransferConexion conexion){
		sqlHTML+="<p><b>"+Lenguaje.getMensaje(Lenguaje.TYPES_SECTION)+"</b></p><hr>";
		sql+="\n-- "+Lenguaje.getMensaje(Lenguaje.TYPES_SECTION)+"\n";
		
		Iterator<Enumerado> tablasD=tiposEnumerados.values().iterator();
		while (tablasD.hasNext()){
			Enumerado e =tablasD.next();
			sqlHTML+=e.codigoHTMLCreacionDeEnum(conexion);
			sql+=e.codigoEstandarCreacionDeEnum(conexion);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void ponClaves(TransferConexion conexion){
		sqlHTML+="<p><b>"+Lenguaje.getMensaje(Lenguaje.KEYS_SECTION)+"</b></p><hr>";
		sql+="\n-- "+Lenguaje.getMensaje(Lenguaje.KEYS_SECTION)+"\n";

//		Iterator tablasE=tablasEntidades.values().iterator();
//		while (tablasE.hasNext()){
//			Tabla t =(Tabla)tablasE.next();
//			sqlHTML+=t.codigoHTMLClavesDeTabla(conexion);
//			sql+=t.codigoEstandarClavesDeTabla(conexion);
//		}
		
		String restEntidad = "";
		String restEntidadHTML = "";

		Iterator tablasE=tablasEntidades.values().iterator();
		while (tablasE.hasNext()){
			Tabla t =(Tabla)tablasE.next();
			if (esPadreEnIsa(t)){
				restEntidadHTML = t.codigoHTMLClavesDeTabla(conexion) + restEntidadHTML;
				restEntidad = t.codigoEstandarClavesDeTabla(conexion) + restEntidad;
			}else{
				restEntidadHTML+=t.codigoHTMLClavesDeTabla(conexion);
				restEntidad+=t.codigoEstandarClavesDeTabla(conexion);
			}
		}
		
		sql += restEntidad;
		sqlHTML += restEntidadHTML;
		
		Iterator tablasR=tablasRelaciones.values().iterator();
		while (tablasR.hasNext()){
			Tabla t =(Tabla)tablasR.next();
			sqlHTML+=t.codigoHTMLClavesDeTabla(conexion);
			sql+=t.codigoEstandarClavesDeTabla(conexion);
		}
		
		Iterator tablasM=tablasMultivalorados.iterator();
		while (tablasM.hasNext()){
			Tabla t =(Tabla)tablasM.next();
			sqlHTML+=t.codigoHTMLClavesDeTabla(conexion);
			sql+=t.codigoEstandarClavesDeTabla(conexion);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void ponRestricciones(){
		sqlHTML+="<p><b>"+Lenguaje.getMensaje(Lenguaje.CONSTRAINTS_SECTION)+"</b></p><hr>";
		sql+="\n-- "+Lenguaje.getMensaje(Lenguaje.CONSTRAINTS_SECTION)+"\n";
		
		// Escribir restricciones de entidad
		DAOEntidades daoEntidades = new DAOEntidades(controlador.getPath());
		Vector<TransferEntidad> entidades = daoEntidades.ListaDeEntidades();
		
		for (int i=0; i < entidades.size(); i++){
			Vector<String> rests = entidades.get(i).getListaRestricciones();
			for (int j=0; j < rests.size(); j++){
				sqlHTML += rests.get(j) + ";" + HTMLUtils.newLine();
				sql += rests.get(j) + "; \n";
			}
		}
		
		// Escribir restricciones de relación
		DAORelaciones daoRelaciones = new DAORelaciones(controlador.getPath());
		Vector<TransferRelacion> relaciones = daoRelaciones.ListaDeRelaciones();
		
		for (int i=0; i < relaciones.size(); i++){
			Vector<String> rests = relaciones.get(i).getListaRestricciones();
			for (int j=0; j < rests.size(); j++){
				sqlHTML += rests.get(j) + ";" + HTMLUtils.newLine();
				sql += rests.get(j) + "; \n";
			}
		}
		
		// Escribir restricciones de atributo
		DAOAtributos daoAtributos = new DAOAtributos(controlador.getPath());
		Vector<TransferAtributo> atributos = daoAtributos.ListaDeAtributos();
		
		for (int i=0; i < atributos.size(); i++){
			Vector<String> rests = atributos.get(i).getListaRestricciones();
			for (int j=0; j < rests.size(); j++){
				sqlHTML += rests.get(j) + ";" + HTMLUtils.newLine();
				sql += rests.get(j) + "; \n";
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void generaModeloRelacional(){
		if (!modeloValidado){
			JOptionPane.showMessageDialog(null,
					Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
					Lenguaje.getMensaje(Lenguaje.MUST_VALIDATE_MODEL),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.PLAIN_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			return;
		}
		
		this.generaTablasEntidades();
		this.generaTablasRelaciones();							
		String mr="<p align=\"center\"><b><font size=\"5\">"+Lenguaje.getMensaje(Lenguaje.RELATIONAL_MODEL_GENERATED)+":</font></b></p>";
		Iterator tablasE=tablasEntidades.values().iterator();
		while (tablasE.hasNext()){
			Tabla t =(Tabla)tablasE.next();
			mr+=t.modeloRelacionalDeTabla();
			//controlador.mensajeDesde_SS(TC.SS_GeneracionModeloRelacional,t.modeloRelacionalDeTabla());
		}
		Iterator tablasR=tablasRelaciones.values().iterator();
		while (tablasR.hasNext()){
			Tabla t =(Tabla)tablasR.next();
			mr+=t.modeloRelacionalDeTabla();
			//controlador.mensajeDesde_SS(TC.SS_GeneracionModeloRelacional,t.modeloRelacionalDeTabla());
		}
		Iterator tablasM=tablasMultivalorados.iterator();
		while (tablasM.hasNext()){
			Tabla t =(Tabla)tablasM.next();
			mr+=t.modeloRelacionalDeTabla();
			//controlador.mensajeDesde_SS(TC.SS_GeneracionModeloRelacional,t.modeloRelacionalDeTabla());
		}
		//	System.out.println(mr);
		controlador.mensajeDesde_SS(TC.SS_GeneracionModeloRelacional,mr);
	}

	//metodos auxiliares.
	/**
	 * Devuelve la lista de atributos con sus caracteristicas de manera recursiva profundizando en los atributos compuestos.
	 * @param ta El atributo compuesto a tratar.
	 * @param nombreEntidad el nombre de la entidad de la que proviene.
	 * @param procedencia es la cadena de nombres de los atributos padre. 
	 */
	@SuppressWarnings("unchecked")
	private Vector<String[]> atributoCompuesto(TransferAtributo ta,String nombreEntidad,String procedencia) {		
		Vector<TransferAtributo> subs=this.dameAtributosEnTransfer(ta.getListaComponentes());	
		Vector <String[]> lista = new Vector<String[]>();
	
		for (int i=0; i<subs.size();i++){
			TransferAtributo aux=subs.elementAt(i);
			if (aux.getCompuesto()){
				//caso recursivo
				String p="";
				if (procedencia!="") p=procedencia+ta.getNombre()+"_";
				else p=ta.getNombre()+"_";
				lista.addAll((Collection)this.atributoCompuesto(aux, nombreEntidad,p));					
			}
			else{
				//caso base
			String[]trio = new String[3];
			trio[0]=procedencia+ta.getNombre()+"_"+aux.getNombre();
		    trio[1]=aux.getDominio();
		    trio[2]=nombreEntidad;
			lista.add(trio);
			}
		}
		return lista;
	}

	/**
	 * 
	 * @param ta El atributo multivalorado en cuestion
	 * @param idEntidad El identificador de la entidad a la que pertenece.
	 */
	private void atributoMultivalorado(TransferAtributo ta, int idEntidad){
		// sacamos la tabla de la entidad propietaria del atributo.
		Tabla tablaEntidad = tablasEntidades.get(idEntidad);
		
		//creamos la tabla.
		Tabla tablaMulti = new Tabla(tablaEntidad.getNombreTabla() + 
										"_" + ta.getNombre());

		// añadimos el campo del atributo, incluso teniendo en cuenta que sea
		// compuesto.
		if (ta.getCompuesto())
			tablaMulti.añadeListaAtributos(this.atributoCompuesto(ta,
					tablaEntidad.getNombreTabla(), ""), tiposEnumerados);
		else
			tablaMulti.añadeAtributo(ta.getNombre(), ta.getDominio(),
					tablaEntidad.getNombreTabla(), tiposEnumerados, ta.getUnique(), ta.getNotnull());
		tablaMulti.añadeListaAtributos(tablaEntidad.getPrimaries(), tiposEnumerados);
		
		Vector<String[]> clavesEntidad = tablaEntidad.getPrimaries();
		String[] referenciadas = new String[clavesEntidad.size()];
		for (int q=0; q<clavesEntidad.size(); q++){
			referenciadas[q] = clavesEntidad.get(q)[0];
		}
		tablaMulti.añadeListaClavesForaneas(tablaEntidad.getPrimaries(),
				tablaEntidad.getNombreTabla(), referenciadas);
		tablasMultivalorados.add(tablaMulti);
	}



	private int objectToInt(Object ob){
		return Integer.parseInt((String)ob);
	}


	@SuppressWarnings("unchecked")
	private  Vector<TransferAtributo> dameAtributosEnTransfer(Vector sinParam){
		DAOAtributos daoAtributos= new DAOAtributos(this.controlador.getPath());
		Vector<TransferAtributo> claves= new Vector<TransferAtributo>(); 
		TransferAtributo aux = new TransferAtributo();
		for (int i=0; i<sinParam.size();i++){
			aux.setIdAtributo(this.objectToInt(sinParam.elementAt(i)));
			aux=daoAtributos.consultarAtributo(aux);
			claves.add(aux);
		}
		return claves;	
	}	
	
	public void compruebaConexion(TransferConexion tc){
		// Ejecutar en DBMS
		System.out.println("Datos de conexión a la base de datos");
		System.out.println("------------------------------------");
		System.out.println("DBMS: " + tc.getRuta() + "(" + tc.getTipoConexion() + ")");
		System.out.println("Usuario: " + tc.getUsuario());
		// System.out.println("Password: " + tc.getPassword());
		
		System.out.println("Intentando conectar...");
		ConectorDBMS conector = FactoriaConectores.obtenerConector(tc.getTipoConexion());
		try {
			conector.abrirConexion(tc.getRuta(), tc.getUsuario(), tc.getPassword());
			conector.cerrarConexion();
		} catch (SQLException e) {
			// Avisar por GUI que falló
			JOptionPane.showMessageDialog(null,
					Lenguaje.getMensaje(Lenguaje.ERROR)+".\n" +
					Lenguaje.getMensaje(Lenguaje.NO_DB_CONEXION)+" \n" +
					Lenguaje.getMensaje(Lenguaje.REASON)+": \n" + e.getMessage(),
					Lenguaje.getMensaje(Lenguaje.DBCASE),
					JOptionPane.PLAIN_MESSAGE,
					new ImageIcon(getClass().getClassLoader().getResource(ImagePath.ERROR)));
			
			// Terminar
			return;
		}
		
		// Avisar por GUI que va bien
		JOptionPane.showMessageDialog(null,
				Lenguaje.getMensaje(Lenguaje.OK_SCRIPT_EXECUT),
				Lenguaje.getMensaje(Lenguaje.DBCASE),
				JOptionPane.PLAIN_MESSAGE,
				new ImageIcon(getClass().getClassLoader().getResource(ImagePath.OK)));
		
		// Terminar
		return;
	}

	public Controlador getControlador() {
		return controlador;
	}

	public void setControlador(Controlador controlador) {
		this.controlador = controlador;
	}
}

